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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public BatchProcessingResult pollOnce() {
        int batchSize = schedulerProperties.getBatchSize();
        List<TrackedLink> batch = trackedLinkStorage.findNextBatchForCheck(batchSize);

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

        int successCount =
                (int) results.stream().filter(LinkProcessingResult::success).count();
        int failedCount = results.size() - successCount;
        int updatedLinksCount = (int)
                results.stream().filter(LinkProcessingResult::updatesFound).count();

        BatchProcessingResult result =
                new BatchProcessingResult(results.size(), successCount, failedCount, updatedLinksCount, errors);

        log.info(
                "Опрос пакета завершён: total={}, success={}, failed={}, updated={}",
                result.total(),
                result.successCount(),
                result.failedCount(),
                result.updatedLinksCount());

        return result;
    }

    private LinkProcessingResult processSingleLink(TrackedLink trackedLink) {
        LinkUpdateDetector detector = selectDetector(trackedLink.getUrl());

        List<LinkEvent> events = detector.detectUpdates(trackedLink);

        if (events.isEmpty()) {
            trackedLinkStorage.updateCheckTime(trackedLink.getId(), OffsetDateTime.now(ZoneOffset.UTC));

            log.debug("Не найдено обновлений для ссылки id={}, url={}", trackedLink.getId(), trackedLink.getUrl());

            return new LinkProcessingResult(trackedLink.getId(), trackedLink.getUrl(), true, false, 0, List.of(), null);
        }

        OffsetDateTime latestEventTime = events.stream()
                .map(LinkEvent::getCreatedAt)
                .max(OffsetDateTime::compareTo)
                .orElse(trackedLink.getLastUpdateTime());

        List<Long> chatIds = trackedLinkStorage.findSubscriberChatIds(trackedLink.getId());

        if (chatIds.isEmpty()) {
            trackedLinkStorage.updateLastUpdateTime(trackedLink.getId(), latestEventTime);
            trackedLinkStorage.updateCheckTime(trackedLink.getId(), OffsetDateTime.now(ZoneOffset.UTC));

            log.debug(
                    "Найдены события, но нет подписчиков для ссылки id={}, url={}",
                    trackedLink.getId(),
                    trackedLink.getUrl());

            return new LinkProcessingResult(
                    trackedLink.getId(), trackedLink.getUrl(), true, true, events.size(), List.of(), null);
        }

        notificationService.sendUpdates(trackedLink, chatIds, events);

        trackedLinkStorage.updateLastUpdateTime(trackedLink.getId(), latestEventTime);
        trackedLinkStorage.updateCheckTime(trackedLink.getId(), OffsetDateTime.now(ZoneOffset.UTC));

        log.debug(
                "Обработана ссылка id={}, url={}, events={}", trackedLink.getId(), trackedLink.getUrl(), events.size());

        return new LinkProcessingResult(
                trackedLink.getId(), trackedLink.getUrl(), true, true, events.size(), chatIds, null);
    }

    private LinkUpdateDetector selectDetector(String url) {
        return linkUpdateDetectors.stream()
                .filter(detector -> detector.supports(url))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Детектор для данной ссылки не найден: " + url));
    }

    private List<LinkProcessingResult> processBatch(List<TrackedLink> batch) {
        int parallelism = schedulerProperties.getParallelism();

        if (parallelism <= 1 || batch.size() <= 1) {
            return batch.stream().map(this::safeProcessSingleLink).toList();
        }

        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        try {
            List<Future<LinkProcessingResult>> futures = batch.stream()
                    .map(link -> executor.submit(() -> safeProcessSingleLink(link)))
                    .toList();

            List<LinkProcessingResult> results = new ArrayList<>();
            for (Future<LinkProcessingResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    log.error("Ошибка получения результата из worker", e);
                }
            }
            return results;
        } finally {
            executor.shutdown();
        }
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

            trackedLinkStorage.updateCheckTime(trackedLink.getId(), OffsetDateTime.now(ZoneOffset.UTC));

            String errorMessage = buildUserFriendlyErrorMessage(trackedLink.getUrl(), e);

            return new LinkProcessingResult(
                    trackedLink.getId(), trackedLink.getUrl(), false, false, 0, chatIds, errorMessage);
        }
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
