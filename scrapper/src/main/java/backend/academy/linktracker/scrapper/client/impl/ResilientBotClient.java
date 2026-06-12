package backend.academy.linktracker.scrapper.client.impl;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.ChatNotification;
import backend.academy.linktracker.scrapper.dto.ChatNotificationBatch;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.LinkUpdateBatch;
import backend.academy.linktracker.scrapper.resilience.HttpResilienceExecutor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResilientBotClient implements BotClient {

    private static final String CLIENT_NAME = "bot";

    private final BotClient delegate;
    private final HttpResilienceExecutor executor;

    @Override
    public void sendUpdate(LinkUpdate update) {
        executor.executeVoid(CLIENT_NAME, () -> delegate.sendUpdate(update));
    }

    @Override
    public void sendUpdates(LinkUpdateBatch batch) {
        executor.executeVoid(CLIENT_NAME, () -> delegate.sendUpdates(batch));
    }

    @Override
    public void sendNotification(ChatNotification notification) {
        executor.executeVoid(CLIENT_NAME, () -> delegate.sendNotification(notification));
    }

    @Override
    public void sendNotifications(ChatNotificationBatch batch) {
        executor.executeVoid(CLIENT_NAME, () -> delegate.sendNotifications(batch));
    }
}
