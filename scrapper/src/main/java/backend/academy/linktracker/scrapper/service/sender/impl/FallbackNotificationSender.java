package backend.academy.linktracker.scrapper.service.sender.impl;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.service.sender.NotificationSender;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.notification.transport", havingValue = "HTTP")
public class FallbackNotificationSender implements NotificationSender {

    private final HttpNotificationSender httpNotificationSender;
    private final KafkaNotificationSender kafkaNotificationSender;

    @Override
    public void sendUpdates(List<LinkUpdate> updates) {
        try {
            httpNotificationSender.sendUpdates(updates);
        } catch (RuntimeException exception) {
            log.warn("HTTP-транспорт уведомлений недоступен, переключаемся на Kafka", exception);
            kafkaNotificationSender.sendUpdates(updates);
        }
    }
}
