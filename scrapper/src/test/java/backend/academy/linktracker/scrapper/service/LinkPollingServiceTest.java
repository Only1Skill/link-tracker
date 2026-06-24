package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.dto.BatchProcessingResult;
import backend.academy.linktracker.scrapper.model.LinkEvent;
import backend.academy.linktracker.scrapper.model.LinkEventType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.TrackedLinkStorage;
import backend.academy.linktracker.scrapper.service.detector.LinkUpdateDetector;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkPollingServiceTest {

    private static final int BATCH_SIZE = 2;
    private static final OffsetDateTime LAST_CHECK_TIME = OffsetDateTime.parse("2026-04-16T10:00:00Z");
    private static final OffsetDateTime LAST_UPDATE_TIME = OffsetDateTime.parse("2026-04-16T10:00:00Z");

    @Mock
    private TrackedLinkStorage trackedLinkStorage;

    @Mock
    private PollingFailureReportService pollingFailureReportService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private LinkUpdateDetector linkUpdateDetector;

    private LinkPollingService linkPollingService;

    @BeforeEach
    void setUp() {
        SchedulerProperties schedulerProperties = new SchedulerProperties();
        schedulerProperties.setBatchSize(BATCH_SIZE);
        schedulerProperties.setParallelism(1);
        schedulerProperties.setInterval(Duration.ofSeconds(10));

        ExecutorService executorService = Executors.newFixedThreadPool(1);

        linkPollingService = new LinkPollingService(
                trackedLinkStorage,
                notificationService,
                schedulerProperties,
                List.of(linkUpdateDetector),
                pollingFailureReportService,
                executorService);
    }

    @Test
    void pollOnce_shouldProcessBatchAndSendNotifications_whenEventsFound() {
        TrackedLink trackedLink = githubLink(1L, "https://github.com/test-owner/test-repo");

        LinkEvent event = githubIssueEvent(
                trackedLink,
                "Issue",
                "alice",
                OffsetDateTime.parse("2026-04-16T11:00:00Z"),
                "https://github.com/test-owner/test-repo/issues/1");

        when(trackedLinkStorage.findNextBatchForCheck(eq(BATCH_SIZE), any(OffsetDateTime.class)))
                .thenReturn(List.of(trackedLink));
        when(linkUpdateDetector.supports(trackedLink.getUrl())).thenReturn(true);
        when(linkUpdateDetector.detectUpdates(trackedLink)).thenReturn(List.of(event));
        when(trackedLinkStorage.findSubscriberChatIds(1L)).thenReturn(List.of(100L, 200L));

        BatchProcessingResult result = linkPollingService.pollOnce();

        assertEquals(1, result.total());
        assertEquals(1, result.successCount());
        assertEquals(0, result.failedCount());
        assertEquals(1, result.updatedLinksCount());

        verify(notificationService).sendUpdates(trackedLink, List.of(100L, 200L), List.of(event));
        verify(trackedLinkStorage)
                .updateProcessingState(
                        eq(1L), any(OffsetDateTime.class), eq(OffsetDateTime.parse("2026-04-16T11:00:00Z")));
        verify(pollingFailureReportService, never()).sendFailureReport(anyList());
    }

    @Test
    void pollOnce_shouldUpdateOnlyCheckTime_whenNoEventsFound() {
        TrackedLink trackedLink = githubLink(1L, "https://github.com/test-owner/test-repo");

        when(trackedLinkStorage.findNextBatchForCheck(eq(BATCH_SIZE), any(OffsetDateTime.class)))
                .thenReturn(List.of(trackedLink));
        when(linkUpdateDetector.supports(trackedLink.getUrl())).thenReturn(true);
        when(linkUpdateDetector.detectUpdates(trackedLink)).thenReturn(List.of());

        BatchProcessingResult result = linkPollingService.pollOnce();

        assertEquals(1, result.total());
        assertEquals(1, result.successCount());
        assertEquals(0, result.failedCount());
        assertEquals(0, result.updatedLinksCount());

        verify(notificationService, never()).sendUpdates(any(), anyList(), anyList());
        verify(trackedLinkStorage)
                .updateProcessingState(eq(1L), any(OffsetDateTime.class), eq(trackedLink.getLastUpdateTime()));
        verify(pollingFailureReportService, never()).sendFailureReport(anyList());
    }

    @Test
    void pollOnce_shouldContinueProcessingOtherLinks_whenOneLinkFails() {
        TrackedLink firstLink = githubLink(1L, "https://github.com/test-owner/test-repo");
        TrackedLink secondLink = githubLink(2L, "https://github.com/test-owner/another-repo");

        LinkEvent secondEvent = githubIssueEvent(
                secondLink,
                "Second issue",
                "bob",
                OffsetDateTime.parse("2026-04-16T11:30:00Z"),
                "https://github.com/test-owner/another-repo/issues/1");

        when(trackedLinkStorage.findNextBatchForCheck(eq(BATCH_SIZE), any(OffsetDateTime.class)))
                .thenReturn(List.of(firstLink, secondLink));
        when(linkUpdateDetector.supports(anyString())).thenReturn(true);
        when(linkUpdateDetector.detectUpdates(firstLink)).thenThrow(new RuntimeException("Boom"));
        when(linkUpdateDetector.detectUpdates(secondLink)).thenReturn(List.of(secondEvent));
        when(trackedLinkStorage.findSubscriberChatIds(1L)).thenReturn(List.of());
        when(trackedLinkStorage.findSubscriberChatIds(2L)).thenReturn(List.of(300L));

        BatchProcessingResult result = linkPollingService.pollOnce();

        assertEquals(2, result.total());
        assertEquals(1, result.successCount());
        assertEquals(1, result.failedCount());
        assertEquals(1, result.updatedLinksCount());

        verify(notificationService).sendUpdates(secondLink, List.of(300L), List.of(secondEvent));

        verify(trackedLinkStorage)
                .updateProcessingState(eq(1L), any(OffsetDateTime.class), eq(firstLink.getLastUpdateTime()));
        verify(trackedLinkStorage)
                .updateProcessingState(
                        eq(2L), any(OffsetDateTime.class), eq(OffsetDateTime.parse("2026-04-16T11:30:00Z")));

        verify(pollingFailureReportService).sendFailureReport(anyList());
    }

    @Test
    void pollOnce_shouldNotSendNotifications_whenNoSubscribers() {
        TrackedLink trackedLink = githubLink(1L, "https://github.com/test-owner/test-repo");

        LinkEvent event = githubIssueEvent(
                trackedLink,
                "Issue",
                "alice",
                OffsetDateTime.parse("2026-04-16T11:00:00Z"),
                "https://github.com/test-owner/test-repo/issues/1");

        when(trackedLinkStorage.findNextBatchForCheck(eq(BATCH_SIZE), any(OffsetDateTime.class)))
                .thenReturn(List.of(trackedLink));
        when(linkUpdateDetector.supports(trackedLink.getUrl())).thenReturn(true);
        when(linkUpdateDetector.detectUpdates(trackedLink)).thenReturn(List.of(event));
        when(trackedLinkStorage.findSubscriberChatIds(1L)).thenReturn(List.of());

        BatchProcessingResult result = linkPollingService.pollOnce();

        assertEquals(1, result.total());
        assertEquals(1, result.successCount());
        assertEquals(0, result.failedCount());
        assertEquals(1, result.updatedLinksCount());

        verify(notificationService, never()).sendUpdates(any(), anyList(), anyList());
        verify(trackedLinkStorage)
                .updateProcessingState(
                        eq(1L), any(OffsetDateTime.class), eq(OffsetDateTime.parse("2026-04-16T11:00:00Z")));
        verify(pollingFailureReportService, never()).sendFailureReport(anyList());
    }

    @Test
    void pollOnce_shouldCollectFailureAndNotAdvanceLastUpdateTime_whenNotificationFails() {
        TrackedLink trackedLink = githubLink(1L, "https://github.com/test-owner/test-repo");

        LinkEvent event = githubIssueEvent(
                trackedLink,
                "Issue",
                "alice",
                OffsetDateTime.parse("2026-04-16T11:00:00Z"),
                "https://github.com/test-owner/test-repo/issues/1");

        when(trackedLinkStorage.findNextBatchForCheck(eq(BATCH_SIZE), any(OffsetDateTime.class)))
                .thenReturn(List.of(trackedLink));
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
        assertEquals(0, result.updatedLinksCount());

        verify(trackedLinkStorage)
                .updateProcessingState(eq(1L), any(OffsetDateTime.class), eq(trackedLink.getLastUpdateTime()));
        verify(pollingFailureReportService).sendFailureReport(anyList());
    }

    private TrackedLink githubLink(Long id, String url) {
        return TrackedLink.builder()
                .id(id)
                .url(url)
                .lastCheckTime(LAST_CHECK_TIME)
                .lastUpdateTime(LAST_UPDATE_TIME)
                .build();
    }

    private LinkEvent githubIssueEvent(
            TrackedLink trackedLink, String title, String author, OffsetDateTime createdAt, String eventUrl) {
        return LinkEvent.builder()
                .linkId(trackedLink.getId())
                .url(trackedLink.getUrl())
                .type(LinkEventType.GITHUB_ISSUE)
                .title(title)
                .author(author)
                .createdAt(createdAt)
                .content("body")
                .eventUrl(eventUrl)
                .build();
    }
}
