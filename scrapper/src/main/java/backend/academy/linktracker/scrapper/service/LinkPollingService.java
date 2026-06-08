package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.BatchProcessingResult;
import backend.academy.linktracker.scrapper.dto.LinkProcessingResult;
import backend.academy.linktracker.scrapper.model.LinkEvent;
import backend.academy.linktracker.scrapper.model.LinkProcessingError;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.TrackedLinkStorage;
import backend.academy.linktracker.scrapper.service.detector.LinkUpdateDetector;
import backend.academy.linktracker.scrapper.util.LinkParser;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkPollingService {

    private final TrackedLinkStorage trackedLinkStorage;
    private final NotificationService notificationService;
    private final SchedulerProperties schedulerProperties;
    private final List<LinkUpdateDetector> linkUpdateDetectors;
    private final PollingFailureReportService pollingFailureReportService;

    @Qualifier("linkPollingExecutor")
    private final ExecutorService linkPollingExecutor;

    public BatchProcessingResult pollOnce() {
        int batchSize = schedulerProperties.getBatchSize();
        OffsetDateTime checkBefore =
                OffsetDateTime.now(ZoneOffset.UTC).minus(schedulerProperties.getInterval());

        List<TrackedLink> batch = trackedLinkStorage.findNextBatchForCheck(batchSize, checkBefore);

        log.info("Запуск опроса пакета, size={}", batch.size());

        if (batch.isEmpty()) {
            return new BatchProcessingResult(0, 0, 0, 0, List.of());
        }

        List<LinkProcessingResult> results = processBatch(batch);

        List<LinkProcessingError> errors = results.stream()
                .filter(result -> !result.success())
                .map(result -> LinkProcessingError.builder()
                        .linkId(result.linkId())
                        .url(result.url())
                        .reason(result.errorMessage())
                        .chatIds(result.subscriberChatIds())
                        .build())
                .toList();

        if (!errors.isEmpty()) {
            try {
                pollingFailureReportService.sendFailureReport(errors);
            } catch (Exception e) {
                log.error("Не удалось отправить отчёт о неуспешной обработке ссылок", e);
            }
        }

        int successCount = (int) results.stream().filter(LinkProcessingResult::success).count();
        int failedCount = results.size() - successCount;
        int updatedLinksCount = (int) results.stream()
                .filter(LinkProcessingResult::updatesFound)
                .count();

        BatchProcessingResult result =
                new BatchProcessingResult(results.size(), successCount, failedCount, updatedLinksCount, errors);

        log.info(
                "Опрос пакета завершён: total={}, success={}, failed={}, updated={}",
                result.total(),
                result.successCount(),
                result.failedCount(),
                result.updatedLinksCount()
        );

        return result;
    }

    private List<LinkProcessingResult> processBatch(List<TrackedLink> batch) {
        int parallelism = Math.min(schedulerProperties.getParallelism(), batch.size());

        if (parallelism <= 1) {
            return processChunk(batch);
        }

        List<List<TrackedLink>> chunks = splitIntoChunks(batch, parallelism);

        List<Future<List<LinkProcessingResult>>> futures = chunks.stream()
                .map(chunk -> linkPollingExecutor.submit(() -> processChunk(chunk)))
                .toList();

        List<LinkProcessingResult> results = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            Future<List<LinkProcessingResult>> future = futures.get(i);
            List<TrackedLink> chunk = chunks.get(i);

            try {
                results.addAll(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Ожидание результата worker было прервано", e);
                results.addAll(buildChunkFailureResults(chunk, "Обработка батча была прервана"));
            } catch (ExecutionException e) {
                log.error("Worker завершился с ошибкой", e);
                results.addAll(buildChunkFailureResults(chunk, "Внутренняя ошибка обработки батча"));
            } catch (Exception e) {
                log.error("Не удалось получить результат обработки батча", e);
                results.addAll(buildChunkFailureResults(chunk, "Не удалось получить результат обработки батча"));
            }
        }

        return results;
    }

    private List<LinkProcessingResult> processChunk(List<TrackedLink> chunk) {
        return chunk.stream()
                .map(this::safeProcessSingleLink)
                .toList();
    }

    private List<List<TrackedLink>> splitIntoChunks(List<TrackedLink> batch, int chunkCount) {
        List<List<TrackedLink>> chunks = new ArrayList<>(chunkCount);

        int baseChunkSize = batch.size() / chunkCount;
        int remainder = batch.size() % chunkCount;

        int fromIndex = 0;
        for (int i = 0; i < chunkCount; i++) {
            int currentChunkSize = baseChunkSize + (i < remainder ? 1 : 0);
            int toIndex = fromIndex + currentChunkSize;
            chunks.add(new ArrayList<>(batch.subList(fromIndex, toIndex)));
            fromIndex = toIndex;
        }

        return chunks;
    }

    private List<LinkProcessingResult> buildChunkFailureResults(List<TrackedLink> chunk, String errorMessage) {
        return chunk.stream()
                .map(link -> {
                    List<Long> chatIds;
                    try {
                        chatIds = trackedLinkStorage.findSubscriberChatIds(link.getId());
                    } catch (Exception ignored) {
                        chatIds = List.of();
                    }

                    return new LinkProcessingResult(
                            link.getId(),
                            link.getUrl(),
                            false,
                            false,
                            0,
                            chatIds,
                            errorMessage
                    );
                })
                .toList();
    }

    private LinkProcessingResult processSingleLink(TrackedLink trackedLink) {
        OffsetDateTime checkedAt = OffsetDateTime.now(ZoneOffset.UTC);

        LinkUpdateDetector detector = selectDetector(trackedLink.getUrl());
        List<LinkEvent> events = detector.detectUpdates(trackedLink);

        if (events.isEmpty()) {
            trackedLinkStorage.updateProcessingState(
                    trackedLink.getId(),
                    checkedAt,
                    trackedLink.getLastUpdateTime()
            );

            log.debug("Не найдено обновлений для ссылки id={}, url={}", trackedLink.getId(), trackedLink.getUrl());

            return new LinkProcessingResult(
                    trackedLink.getId(),
                    trackedLink.getUrl(),
                    true,
                    false,
                    0,
                    List.of(),
                    null
            );
        }

        OffsetDateTime latestEventTime = events.stream()
                .map(LinkEvent::getCreatedAt)
                .max(OffsetDateTime::compareTo)
                .orElse(trackedLink.getLastUpdateTime());

        List<Long> chatIds = trackedLinkStorage.findSubscriberChatIds(trackedLink.getId());

        if (chatIds.isEmpty()) {
            trackedLinkStorage.updateProcessingState(
                    trackedLink.getId(),
                    checkedAt,
                    latestEventTime
            );

            log.debug(
                    "Найдены события, но нет подписчиков для ссылки id={}, url={}",
                    trackedLink.getId(),
                    trackedLink.getUrl()
            );

            return new LinkProcessingResult(
                    trackedLink.getId(),
                    trackedLink.getUrl(),
                    true,
                    true,
                    events.size(),
                    List.of(),
                    null
            );
        }

        notificationService.sendUpdates(trackedLink, chatIds, events);

        trackedLinkStorage.updateProcessingState(
                trackedLink.getId(),
                checkedAt,
                latestEventTime
        );

        log.debug(
                "Обработана ссылка id={}, url={}, events={}",
                trackedLink.getId(),
                trackedLink.getUrl(),
                events.size()
        );

        return new LinkProcessingResult(
                trackedLink.getId(),
                trackedLink.getUrl(),
                true,
                true,
                events.size(),
                chatIds,
                null
        );
    }

    private LinkProcessingResult safeProcessSingleLink(TrackedLink trackedLink) {
        try {
            return processSingleLink(trackedLink);
        } catch (Exception e) {
            log.error("Не удалось обработать ссылку id={}, url={}", trackedLink.getId(), trackedLink.getUrl(), e);

            List<Long> chatIds;
            try {
                chatIds = trackedLinkStorage.findSubscriberChatIds(trackedLink.getId());
            } catch (Exception ignored) {
                chatIds = List.of();
            }

            try {
                trackedLinkStorage.updateProcessingState(
                        trackedLink.getId(),
                        OffsetDateTime.now(ZoneOffset.UTC),
                        trackedLink.getLastUpdateTime()
                );
            } catch (Exception updateException) {
                log.error(
                        "Не удалось обновить время проверки для ссылки id={}, url={}",
                        trackedLink.getId(),
                        trackedLink.getUrl(),
                        updateException
                );
            }

            String errorMessage = buildUserFriendlyErrorMessage(trackedLink.getUrl(), e);

            return new LinkProcessingResult(
                    trackedLink.getId(),
                    trackedLink.getUrl(),
                    false,
                    false,
                    0,
                    chatIds,
                    errorMessage
            );
        }
    }

    private LinkUpdateDetector selectDetector(String url) {
        return linkUpdateDetectors.stream()
                .filter(detector -> detector.supports(url))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Детектор для данной ссылки не найден: " + url));
    }

    private String buildUserFriendlyErrorMessage(String url, Exception e) {
        if (e instanceof HttpClientErrorException.NotFound) {
            if (LinkParser.parseGitHub(url) != null) {
                return "GitHub-репозиторий не найден или недоступен";
            }
            if (LinkParser.parseStackOverflow(url) != null) {
                return "Вопрос StackOverflow не найден или недоступен";
            }
            return "Ресурс не найден";
        }

        if (e instanceof HttpClientErrorException.Forbidden) {
            return "Внешний сервис временно запретил доступ к ресурсу";
        }

        if (e instanceof ResourceAccessException) {
            return "Не удалось связаться с внешним сервисом";
        }

        return "Не удалось проверить ссылку. Попробуем снова позже.";
    }
}
