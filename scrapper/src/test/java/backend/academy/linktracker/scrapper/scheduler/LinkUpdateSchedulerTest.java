package backend.academy.linktracker.scrapper.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.dto.BatchProcessingResult;
import backend.academy.linktracker.scrapper.service.LinkPollingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkUpdateSchedulerTest {

    @Mock
    private LinkPollingService linkPollingService;

    @InjectMocks
    private LinkUpdateScheduler scheduler;

    @Test
    void updateLinks_shouldDelegateToPollingService() {
        when(linkPollingService.pollOnce()).thenReturn(new BatchProcessingResult(0, 0, 0, 0, List.of()));

        scheduler.pollLinks();

        verify(linkPollingService).pollOnce();
    }
}
