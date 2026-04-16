package backend.academy.linktracker.scrapper.client.impl;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.ChatNotification;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
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
}
