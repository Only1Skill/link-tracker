package backend.academy.linktracker.service;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.dto.LinkUpdate;
import backend.academy.linktracker.exception.InvalidLinkUpdateMessageException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkUpdateKafkaConsumer {

    private final TelegramClient telegramClient;
    private final Validator validator;

    @KafkaListener(
            topics = "${app.kafka.topic.link-updates}",
            containerFactory = "linkUpdateKafkaListenerContainerFactory")
    public void consume(LinkUpdate update) {
        validate(update);

        for (Long chatId : update.tgChatIds()) {
            telegramClient.sendMessage(chatId, update.description());
        }

        log.info(
                "Kafka notification обработана, linkId={}, recipients={}",
                update.id(),
                update.tgChatIds().size());
    }

    private void validate(LinkUpdate update) {
        if (update == null) {
            throw new InvalidLinkUpdateMessageException("Kafka-сообщение LinkUpdate не должно быть null");
        }

        if (update.id() == null) {
            throw new InvalidLinkUpdateMessageException("ID ссылки не должен быть null");
        }

        if (update.url() == null || update.url().isBlank()) {
            throw new InvalidLinkUpdateMessageException("URL ссылки не должен быть пустым");
        }

        if (update.description() == null || update.description().isBlank()) {
            throw new InvalidLinkUpdateMessageException("Описание обновления не должно быть пустым");
        }

        if (update.tgChatIds() == null || update.tgChatIds().isEmpty()) {
            throw new InvalidLinkUpdateMessageException("Список получателей не должен быть пустым");
        }

        Set<ConstraintViolation<LinkUpdate>> violations = validator.validate(update);

        if (!violations.isEmpty()) {
            throw new InvalidLinkUpdateMessageException("Некорректное Kafka-сообщение LinkUpdate: " + violations);
        }
    }
}
