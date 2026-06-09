package backend.academy.linktracker.controller;

import static org.mockito.Mockito.verify;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.dto.ChatNotification;
import backend.academy.linktracker.dto.ChatNotificationBatch;
import backend.academy.linktracker.dto.LinkUpdate;
import backend.academy.linktracker.dto.LinkUpdateBatch;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BotUpdateControllerTest {

    private final TelegramClient telegramClient = Mockito.mock(TelegramClient.class);
    private final BotUpdateController controller = new BotUpdateController(telegramClient);

    @Test
    void sendUpdate_shouldSendMessageToEveryChatId() {
        LinkUpdate update =
                new LinkUpdate(1L, "https://github.com/test-owner/test-repo", "New issue title", List.of(100L, 200L));

        controller.sendUpdate(update);

        verify(telegramClient).sendMessage(100L, "New issue title");
        verify(telegramClient).sendMessage(200L, "New issue title");
    }

    @Test
    void sendUpdates_shouldSendMessagesForEveryUpdateInBatch() {
        LinkUpdate firstUpdate =
                new LinkUpdate(1L, "https://github.com/test-owner/test-repo", "First message", List.of(100L, 200L));

        LinkUpdate secondUpdate = new LinkUpdate(
                2L, "https://stackoverflow.com/questions/12345/how-to-write-tests", "Second message", List.of(300L));

        controller.sendUpdates(new LinkUpdateBatch(List.of(firstUpdate, secondUpdate)));

        verify(telegramClient).sendMessage(100L, "First message");
        verify(telegramClient).sendMessage(200L, "First message");
        verify(telegramClient).sendMessage(300L, "Second message");
    }

    @Test
    void sendNotification_shouldSendServiceMessageToEveryChatId() {
        ChatNotification notification = new ChatNotification("Не удалось проверить ссылку", List.of(100L, 200L));

        controller.sendNotification(notification);

        verify(telegramClient).sendMessage(100L, "Не удалось проверить ссылку");
        verify(telegramClient).sendMessage(200L, "Не удалось проверить ссылку");
    }

    @Test
    void sendNotifications_shouldSendMessagesForEveryNotificationInBatch() {
        ChatNotification firstNotification = new ChatNotification("Ошибка GitHub", List.of(100L));

        ChatNotification secondNotification = new ChatNotification("Ошибка StackOverflow", List.of(200L, 300L));

        controller.sendNotifications(new ChatNotificationBatch(List.of(firstNotification, secondNotification)));

        verify(telegramClient).sendMessage(100L, "Ошибка GitHub");
        verify(telegramClient).sendMessage(200L, "Ошибка StackOverflow");
        verify(telegramClient).sendMessage(300L, "Ошибка StackOverflow");
    }
}
