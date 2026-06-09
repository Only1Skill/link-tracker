package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.ChatNotification;
import backend.academy.linktracker.scrapper.dto.ChatNotificationBatch;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.LinkUpdateBatch;

public interface BotClient {
    void sendUpdate(LinkUpdate linkUpdate);

    void sendUpdates(LinkUpdateBatch batch);

    void sendNotification(ChatNotification notification);

    void sendNotifications(ChatNotificationBatch batch);
}
