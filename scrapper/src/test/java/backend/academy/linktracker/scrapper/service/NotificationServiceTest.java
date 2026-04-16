package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.LinkEvent;
import backend.academy.linktracker.scrapper.model.LinkEventType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.service.formatter.UpdateMessageFormatter;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private BotClient botClient;

    @Mock
    private UpdateMessageFormatter updateMessageFormatter;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(botClient, updateMessageFormatter);
    }

    @Test
    void sendUpdates_shouldSendOneLinkUpdatePerEvent() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .lastCheckTime(OffsetDateTime.now())
                .lastUpdateTime(OffsetDateTime.now())
                .build();

        LinkEvent firstEvent = LinkEvent.builder()
                .type(LinkEventType.GITHUB_ISSUE)
                .title("Issue 1")
                .build();

        LinkEvent secondEvent =
                LinkEvent.builder().type(LinkEventType.GITHUB_PR).title("PR 2").build();

        when(updateMessageFormatter.format(firstEvent)).thenReturn("message-1");
        when(updateMessageFormatter.format(secondEvent)).thenReturn("message-2");

        notificationService.sendUpdates(trackedLink, List.of(100L, 200L), List.of(firstEvent, secondEvent));

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);

        verify(botClient, times(2)).sendUpdate(captor.capture());

        List<LinkUpdate> sentUpdates = captor.getAllValues();

        assertThat(sentUpdates.getFirst().id()).isEqualTo(1L);
        assertThat(sentUpdates.getFirst().url()).isEqualTo("https://github.com/test-owner/test-repo");
        assertThat(sentUpdates.getFirst().description()).isEqualTo("message-1");
        assertThat(sentUpdates.getFirst().tgChatIds()).containsExactly(100L, 200L);

        assertThat(sentUpdates.get(1).description()).isEqualTo("message-2");
    }

    @Test
    void sendUpdates_shouldDoNothing_whenEventsAreEmpty() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .build();

        notificationService.sendUpdates(trackedLink, List.of(100L), List.of());

        verify(botClient, never()).sendUpdate(any());
    }

    @Test
    void sendUpdates_shouldDoNothing_whenChatIdsAreEmpty() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .build();

        LinkEvent event = LinkEvent.builder()
                .type(LinkEventType.GITHUB_ISSUE)
                .title("Issue 1")
                .build();

        notificationService.sendUpdates(trackedLink, List.of(), List.of(event));

        verify(botClient, never()).sendUpdate(any());
    }

    @Test
    void sendUpdates_shouldPropagateException_whenBotClientFails() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .build();

        LinkEvent event = LinkEvent.builder()
                .type(LinkEventType.GITHUB_ISSUE)
                .title("Issue 1")
                .build();

        when(updateMessageFormatter.format(event)).thenReturn("message");
        doThrow(new RuntimeException("bot unavailable")).when(botClient).sendUpdate(any());

        assertThrows(
                RuntimeException.class,
                () -> notificationService.sendUpdates(trackedLink, List.of(100L), List.of(event)));
    }
}
