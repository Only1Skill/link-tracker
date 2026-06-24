package backend.academy.linktracker.scrapper.service.sender.impl;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.properties.KafkaTopicProperties;
import backend.academy.linktracker.scrapper.service.sender.NotificationSender;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.transport", havingValue = "KAFKA", matchIfMissing = true)
public class KafkaNotificationSender implements NotificationSender {

    private final KafkaTemplate<Long, LinkUpdate> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    @Override
    public void sendUpdates(List<LinkUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        for (LinkUpdate update : updates) {
            kafkaTemplate.send(topicProperties.getLinkUpdates(), update.id(), update);
        }
    }
}
