package backend.academy.linktracker.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.dto.LinkUpdate;
import backend.academy.linktracker.exception.InvalidLinkUpdateMessageException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LinkUpdateKafkaConsumerTest {

    private TelegramClient telegramClient;
    private LinkUpdateKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        telegramClient = Mockito.mock(TelegramClient.class);

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        consumer = new LinkUpdateKafkaConsumer(telegramClient, validator);
    }

    @Test
    void consume_shouldSendMessageToEveryChatId_whenMessageIsValid() {
        LinkUpdate update =
                new LinkUpdate(1L, "https://github.com/test-owner/test-repo", "New issue title", List.of(100L, 200L));

        consumer.consume(update);

        verify(telegramClient).sendMessage(100L, "New issue title");
        verify(telegramClient).sendMessage(200L, "New issue title");
    }

    @Test
    void consume_shouldThrowExceptionAndNotSendMessage_whenMessageIsInvalid() {
        LinkUpdate update = new LinkUpdate(null, "https://github.com/test-owner/test-repo", "", List.of());

        assertThrows(InvalidLinkUpdateMessageException.class, () -> consumer.consume(update));

        verify(telegramClient, never()).sendMessage(Mockito.anyLong(), Mockito.anyString());
    }
}
