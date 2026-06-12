package backend.academy.linktracker.scrapper.client.impl;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.ChatNotification;
import backend.academy.linktracker.scrapper.dto.ChatNotificationBatch;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.LinkUpdateBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class BotRestClient implements BotClient {
    private final RestClient botRestClient;

    @Override
    public void sendUpdate(LinkUpdate update) {
        botRestClient.post().uri("/updates").body(update).retrieve().toBodilessEntity();

        log.info("Обновление отправлено в bot, linkId={}", update.id());
    }

    @Override
    public void sendUpdates(LinkUpdateBatch batch) {
        if (batch == null || batch.updates().isEmpty()) {
            return;
        }

        botRestClient.post().uri("/updates/batch").body(batch).retrieve().toBodilessEntity();

        log.info("Batch обновлений отправлен в bot, count={}", batch.updates().size());
    }

    @Override
    public void sendNotification(ChatNotification notification) {
        botRestClient.post().uri("/notifications").body(notification).retrieve().toBodilessEntity();

        log.info(
                "Сервисное уведомление отправлено в bot, recipients={}",
                notification.tgChatIds().size());
    }

    @Override
    public void sendNotifications(ChatNotificationBatch batch) {
        if (batch == null || batch.notifications().isEmpty()) {
            return;
        }

        botRestClient.post().uri("/notifications/batch").body(batch).retrieve().toBodilessEntity();

        log.info(
                "Batch сервисных уведомлений отправлен в bot, count={}",
                batch.notifications().size());
    }
}
