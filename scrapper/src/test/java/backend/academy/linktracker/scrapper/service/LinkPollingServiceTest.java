package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.dto.BatchProcessingResult;
import backend.academy.linktracker.scrapper.model.LinkEvent;
import backend.academy.linktracker.scrapper.model.LinkEventType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.TrackedLinkStorage;
import backend.academy.linktracker.scrapper.service.detector.LinkUpdateDetector;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkPollingServiceTest {

    @Mock
    private TrackedLinkStorage trackedLinkStorage;

    @Mock
    private PollingFailureReportService pollingFailureReportService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private LinkUpdateDetector linkUpdateDetector;

    private LinkPollingService linkPollingService;
    private SchedulerProperties schedulerProperties;

    @BeforeEach
    void setUp() {
        schedulerProperties = new SchedulerProperties();
        schedulerProperties.setBatchSize(2);
        schedulerProperties.setParallelism(1);

        linkPollingService = new LinkPollingService(
                trackedLinkStorage,
                notificationService,
                schedulerProperties,
                List.of(linkUpdateDetector),
                pollingFailureReportService);
    }

    @Test
    void pollOnce_shouldProcessBatchAndSendNotifications_whenEventsFound() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .lastCheckTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .lastUpdateTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .build();

        LinkEvent event = LinkEvent.builder()
                .linkId(1L)
                .url(trackedLink.getUrl())
                .type(LinkEventType.GITHUB_ISSUE)
                .title("Issue")
                .author("alice")
                .createdAt(OffsetDateTime.parse("2026-04-16T11:00:00Z"))
                .content("body")
                .eventUrl("https://github.com/test-owner/test-repo/issues/1")
                .build();

        when(trackedLinkStorage.findNextBatchForCheck(2)).thenReturn(List.of(trackedLink));
        when(linkUpdateDetector.supports(trackedLink.getUrl())).thenReturn(true);
        when(linkUpdateDetector.detectUpdates(trackedLink)).thenReturn(List.of(event));
        when(trackedLinkStorage.findSubscriberChatIds(1L)).thenReturn(List.of(100L, 200L));

        linkPollingService.pollOnce();

        verify(notificationService).sendUpdates(trackedLink, List.of(100L, 200L), List.of(event));
        verify(trackedLinkStorage).updateLastUpdateTime(1L, OffsetDateTime.parse("2026-04-16T11:00:00Z"));
        verify(trackedLinkStorage).updateCheckTime(eq(1L), any(OffsetDateTime.class));
    }

    @Test
    void pollOnce_shouldUpdateOnlyCheckTime_whenNoEventsFound() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .lastCheckTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .lastUpdateTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .build();

        when(trackedLinkStorage.findNextBatchForCheck(2)).thenReturn(List.of(trackedLink));
        when(linkUpdateDetector.supports(trackedLink.getUrl())).thenReturn(true);
        when(linkUpdateDetector.detectUpdates(trackedLink)).thenReturn(List.of());

        linkPollingService.pollOnce();

        verify(notificationService, never()).sendUpdates(any(), any(), any());
        verify(trackedLinkStorage, never()).updateLastUpdateTime(anyLong(), any());
        verify(trackedLinkStorage).updateCheckTime(eq(1L), any(OffsetDateTime.class));
    }

    @Test
    void pollOnce_shouldContinueProcessingOtherLinks_whenOneLinkFails() {
        TrackedLink firstLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .lastCheckTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .lastUpdateTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .build();

        TrackedLink secondLink = TrackedLink.builder()
                .id(2L)
                .url("https://github.com/test-owner/another-repo")
                .lastCheckTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .lastUpdateTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .build();

        LinkEvent secondEvent = LinkEvent.builder()
                .linkId(2L)
                .url(secondLink.getUrl())
                .type(LinkEventType.GITHUB_ISSUE)
                .title("Second issue")
                .author("bob")
                .createdAt(OffsetDateTime.parse("2026-04-16T11:30:00Z"))
                .content("body")
                .eventUrl("https://github.com/test-owner/another-repo/issues/1")
                .build();

        when(trackedLinkStorage.findNextBatchForCheck(2)).thenReturn(List.of(firstLink, secondLink));
        when(linkUpdateDetector.supports(anyString())).thenReturn(true);
        when(linkUpdateDetector.detectUpdates(firstLink)).thenThrow(new RuntimeException("Boom"));
        when(linkUpdateDetector.detectUpdates(secondLink)).thenReturn(List.of(secondEvent));
        when(trackedLinkStorage.findSubscriberChatIds(2L)).thenReturn(List.of(300L));

        linkPollingService.pollOnce();

        verify(notificationService).sendUpdates(secondLink, List.of(300L), List.of(secondEvent));
        verify(trackedLinkStorage).updateLastUpdateTime(2L, OffsetDateTime.parse("2026-04-16T11:30:00Z"));
        verify(trackedLinkStorage).updateCheckTime(eq(2L), any(OffsetDateTime.class));
    }

    @Test
    void pollOnce_shouldNotSendNotifications_whenNoSubscribers() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .lastCheckTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .lastUpdateTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .build();

        LinkEvent event = LinkEvent.builder()
                .linkId(1L)
                .url(trackedLink.getUrl())
                .type(LinkEventType.GITHUB_ISSUE)
                .title("Issue")
                .author("alice")
                .createdAt(OffsetDateTime.parse("2026-04-16T11:00:00Z"))
                .content("body")
                .eventUrl("https://github.com/test-owner/test-repo/issues/1")
                .build();

        when(trackedLinkStorage.findNextBatchForCheck(2)).thenReturn(List.of(trackedLink));
        when(linkUpdateDetector.supports(trackedLink.getUrl())).thenReturn(true);
        when(linkUpdateDetector.detectUpdates(trackedLink)).thenReturn(List.of(event));
        when(trackedLinkStorage.findSubscriberChatIds(1L)).thenReturn(List.of());

        linkPollingService.pollOnce();

        verify(notificationService, never()).sendUpdates(any(), any(), any());
        verify(trackedLinkStorage).updateLastUpdateTime(1L, OffsetDateTime.parse("2026-04-16T11:00:00Z"));
        verify(trackedLinkStorage).updateCheckTime(eq(1L), any(OffsetDateTime.class));
    }

    @Test
    void pollOnce_shouldCollectFailureAndNotUpdateLastUpdateTime_whenNotificationFails() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .lastCheckTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .lastUpdateTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .build();

        LinkEvent event = LinkEvent.builder()
                .linkId(1L)
                .url(trackedLink.getUrl())
                .type(LinkEventType.GITHUB_ISSUE)
                .title("Issue")
                .author("alice")
                .createdAt(OffsetDateTime.parse("2026-04-16T11:00:00Z"))
                .content("body")
                .eventUrl("https://github.com/test-owner/test-repo/issues/1")
                .build();

        when(trackedLinkStorage.findNextBatchForCheck(2)).thenReturn(List.of(trackedLink));
        when(linkUpdateDetector.supports(trackedLink.getUrl())).thenReturn(true);
        when(linkUpdateDetector.detectUpdates(trackedLink)).thenReturn(List.of(event));
        when(trackedLinkStorage.findSubscriberChatIds(1L)).thenReturn(List.of(100L));

        doThrow(new RuntimeException("bot unavailable"))
                .when(notificationService)
                .sendUpdates(trackedLink, List.of(100L), List.of(event));

        BatchProcessingResult result = linkPollingService.pollOnce();

        assertEquals(1, result.total());
        assertEquals(0, result.successCount());
        assertEquals(1, result.failedCount());

        verify(trackedLinkStorage, never()).updateLastUpdateTime(anyLong(), any());
        verify(trackedLinkStorage).updateCheckTime(eq(1L), any(OffsetDateTime.class));
        verify(pollingFailureReportService).sendFailureReport(anyList());
    }
}
