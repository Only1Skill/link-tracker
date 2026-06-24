package backend.academy.linktracker.scrapper.service.sender.impl;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.LinkUpdateBatch;
import backend.academy.linktracker.scrapper.service.sender.NotificationSender;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.transport", havingValue = "HTTP")
public class HttpNotificationSender implements NotificationSender {

    private final BotClient botClient;

    @Override
    public void sendUpdates(List<LinkUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        botClient.sendUpdates(new LinkUpdateBatch(updates));
    }
}
