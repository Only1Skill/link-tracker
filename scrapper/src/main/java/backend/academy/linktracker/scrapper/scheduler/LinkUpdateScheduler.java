package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.dto.BatchProcessingResult;
import backend.academy.linktracker.scrapper.service.LinkPollingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class LinkUpdateScheduler {
    private final LinkPollingService linkPollingService;

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void pollLinks() {
        log.debug("Запуск плановой проверки ссылок");

        BatchProcessingResult result = linkPollingService.pollOnce();

        log.info(
                "Плановая проверка завершена: total={}, success={}, failed={}, updated={}",
                result.total(),
                result.successCount(),
                result.failedCount(),
                result.updatedLinksCount());
    }
}
