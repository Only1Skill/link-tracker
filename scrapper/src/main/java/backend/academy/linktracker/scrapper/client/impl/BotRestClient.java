package backend.academy.linktracker.scrapper.client.impl;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.ChatNotification;
import backend.academy.linktracker.scrapper.dto.ChatNotificationBatch;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.LinkUpdateBatch;
import backend.academy.linktracker.scrapper.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class BotRestClient implements BotClient {
    private final RestClient botRestClient;

    @Override
    public void sendUpdate(LinkUpdate update) {
        botRestClient
                .post()
                .uri("/updates")
                .body(update)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (_, res) -> {
                    throw new IllegalStateException("Бот вернул ошибку: " + res.getStatusCode() + " "
                            + LogSanitizer.sanitize(res.getStatusText()));
                })
                .toBodilessEntity();

        log.info("Обновление отправлено в bot, linkId={}", update.id());
    }

    @Override
    public void sendUpdates(LinkUpdateBatch batch) {
        if (batch == null || batch.updates().isEmpty()) {
            return;
        }

        botRestClient
                .post()
                .uri("/updates/batch")
                .body(batch)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (_, res) -> {
                    throw new IllegalStateException("Бот вернул ошибку при отправке batch updates: "
                            + res.getStatusCode() + " " + LogSanitizer.sanitize(res.getStatusText()));
                })
                .toBodilessEntity();

        log.info("Batch обновлений отправлен в bot, count={}", batch.updates().size());
    }

    @Override
    public void sendNotification(ChatNotification notification) {
        botRestClient
                .post()
                .uri("/notifications")
                .body(notification)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (_, res) -> {
                    throw new IllegalStateException("Бот вернул ошибку при отправке notification: "
                            + res.getStatusCode() + " " + LogSanitizer.sanitize(res.getStatusText()));
                })
                .toBodilessEntity();

        log.info(
                "Сервисное уведомление отправлено в bot, recipients={}",
                notification.tgChatIds().size());
    }

    @Override
    public void sendNotifications(ChatNotificationBatch batch) {
        if (batch == null || batch.notifications().isEmpty()) {
            return;
        }

        botRestClient
                .post()
                .uri("/notifications/batch")
                .body(batch)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (_, res) -> {
                    throw new IllegalStateException("Бот вернул ошибку при отправке batch notifications: "
                            + res.getStatusCode() + " " + LogSanitizer.sanitize(res.getStatusText()));
                })
                .toBodilessEntity();

        log.info(
                "Batch сервисных уведомлений отправлен в bot, count={}",
                batch.notifications().size());
    }
}
