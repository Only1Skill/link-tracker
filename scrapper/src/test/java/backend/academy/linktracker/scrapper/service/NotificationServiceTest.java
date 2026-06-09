package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.LinkEvent;
import backend.academy.linktracker.scrapper.model.LinkEventType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.service.formatter.UpdateMessageFormatter;
import backend.academy.linktracker.scrapper.service.sender.NotificationSender;
import java.time.OffsetDateTime;
import java.util.Arrays;
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
    private NotificationSender notificationSender;

    @Mock
    private UpdateMessageFormatter updateMessageFormatter;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(updateMessageFormatter, notificationSender);
    }

    @Test
    void sendUpdates_shouldSendOneUpdatePerEventThroughNotificationSender() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .lastCheckTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .lastUpdateTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .build();

        LinkEvent firstEvent = LinkEvent.builder()
                .type(LinkEventType.GITHUB_ISSUE)
                .title("Issue 1")
                .build();

        LinkEvent secondEvent =
                LinkEvent.builder().type(LinkEventType.GITHUB_PR).title("PR 2").build();

        when(updateMessageFormatter.format(firstEvent)).thenReturn("message-1");
        when(updateMessageFormatter.format(secondEvent)).thenReturn("message-2");

        notificationService.sendUpdates(trackedLink, List.of(100L, 200L, 100L), List.of(firstEvent, secondEvent));

        ArgumentCaptor<List<LinkUpdate>> captor = ArgumentCaptor.forClass(List.class);

        verify(notificationSender).sendUpdates(captor.capture());

        List<LinkUpdate> updates = captor.getValue();

        assertThat(updates).hasSize(2);

        LinkUpdate firstUpdate = updates.get(0);
        LinkUpdate secondUpdate = updates.get(1);

        assertThat(firstUpdate.id()).isEqualTo(1L);
        assertThat(firstUpdate.url()).isEqualTo("https://github.com/test-owner/test-repo");
        assertThat(firstUpdate.description()).isEqualTo("message-1");
        assertThat(firstUpdate.tgChatIds()).containsExactly(100L, 200L);

        assertThat(secondUpdate.id()).isEqualTo(1L);
        assertThat(secondUpdate.url()).isEqualTo("https://github.com/test-owner/test-repo");
        assertThat(secondUpdate.description()).isEqualTo("message-2");
        assertThat(secondUpdate.tgChatIds()).containsExactly(100L, 200L);
    }

    @Test
    void sendUpdates_shouldDoNothing_whenEventsAreEmpty() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .build();

        notificationService.sendUpdates(trackedLink, List.of(100L), List.of());

        verify(notificationSender, never()).sendUpdates(anyList());
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

        verify(notificationSender, never()).sendUpdates(anyList());
    }

    @Test
    void sendUpdates_shouldDoNothing_whenOnlyNullChatIdsProvided() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .build();

        LinkEvent event = LinkEvent.builder()
                .type(LinkEventType.GITHUB_ISSUE)
                .title("Issue 1")
                .build();

        notificationService.sendUpdates(trackedLink, Arrays.asList(null, null), List.of(event));

        verify(notificationSender, never()).sendUpdates(anyList());
    }

    @Test
    void sendUpdates_shouldThrowException_whenTrackedLinkIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> notificationService.sendUpdates(null, List.of(100L), List.of()));

        assertThat(exception.getMessage()).isEqualTo("trackedLink не должна быть пустой");
    }

    @Test
    void sendUpdates_shouldPropagateException_whenNotificationSenderFails() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .build();

        LinkEvent event = LinkEvent.builder()
                .type(LinkEventType.GITHUB_ISSUE)
                .title("Issue 1")
                .build();

        when(updateMessageFormatter.format(event)).thenReturn("message");

        doThrow(new RuntimeException("transport unavailable"))
                .when(notificationSender)
                .sendUpdates(anyList());

        assertThrows(
                RuntimeException.class,
                () -> notificationService.sendUpdates(trackedLink, List.of(100L), List.of(event)));
    }
}
