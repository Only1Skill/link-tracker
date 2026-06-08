package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.ChatNotification;
import backend.academy.linktracker.scrapper.dto.ChatNotificationBatch;
import backend.academy.linktracker.scrapper.model.LinkProcessingError;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PollingFailureReportServiceTest {

    @Mock
    private BotClient botClient;

    private PollingFailureReportService service;

    @BeforeEach
    void setUp() {
        service = new PollingFailureReportService(botClient);
    }

    @Test
    void sendFailureReport_shouldGroupErrorsByChatAndSendOneBatch() {
        LinkProcessingError firstError = LinkProcessingError.builder()
                .linkId(1L)
                .url("https://github.com/a/b")
                .reason("github api unavailable")
                .chatIds(List.of(100L, 200L))
                .build();

        LinkProcessingError secondError = LinkProcessingError.builder()
                .linkId(2L)
                .url("https://stackoverflow.com/questions/1")
                .reason("timeout")
                .chatIds(List.of(100L))
                .build();

        service.sendFailureReport(List.of(firstError, secondError));

        ArgumentCaptor<ChatNotificationBatch> captor = ArgumentCaptor.forClass(ChatNotificationBatch.class);

        verify(botClient).sendNotifications(captor.capture());
        verify(botClient, never()).sendNotification(any());

        ChatNotificationBatch batch = captor.getValue();

        assertThat(batch.notifications()).hasSize(2);

        Map<Long, ChatNotification> notificationsByChat = batch.notifications().stream()
                .collect(Collectors.toMap(
                        notification -> notification.tgChatIds().getFirst(), notification -> notification));

        ChatNotification firstChatNotification = notificationsByChat.get(100L);
        ChatNotification secondChatNotification = notificationsByChat.get(200L);

        assertThat(firstChatNotification).isNotNull();
        assertThat(firstChatNotification.tgChatIds()).containsExactly(100L);
        assertThat(firstChatNotification.message())
                .contains("Не удалось проверить некоторые ссылки:")
                .contains("https://github.com/a/b")
                .contains("github api unavailable")
                .contains("https://stackoverflow.com/questions/1")
                .contains("timeout");

        assertThat(secondChatNotification).isNotNull();
        assertThat(secondChatNotification.tgChatIds()).containsExactly(200L);
        assertThat(secondChatNotification.message())
                .contains("Не удалось проверить некоторые ссылки:")
                .contains("https://github.com/a/b")
                .contains("github api unavailable")
                .doesNotContain("https://stackoverflow.com/questions/1");
    }

    @Test
    void sendFailureReport_shouldDoNothing_whenErrorsAreNull() {
        service.sendFailureReport(null);

        verify(botClient, never()).sendNotifications(any());
        verify(botClient, never()).sendNotification(any());
    }

    @Test
    void sendFailureReport_shouldDoNothing_whenErrorsAreEmpty() {
        service.sendFailureReport(List.of());

        verify(botClient, never()).sendNotifications(any());
        verify(botClient, never()).sendNotification(any());
    }

    @Test
    void sendFailureReport_shouldDoNothing_whenErrorsHaveNoChatIds() {
        LinkProcessingError errorWithoutChats = LinkProcessingError.builder()
                .linkId(1L)
                .url("https://github.com/a/b")
                .reason("github api unavailable")
                .chatIds(List.of())
                .build();

        LinkProcessingError errorWithNullChats = LinkProcessingError.builder()
                .linkId(2L)
                .url("https://stackoverflow.com/questions/1")
                .reason("timeout")
                .chatIds(null)
                .build();

        service.sendFailureReport(List.of(errorWithoutChats, errorWithNullChats));

        verify(botClient, never()).sendNotifications(any());
        verify(botClient, never()).sendNotification(any());
    }
}
