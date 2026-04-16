package backend.academy.linktracker.scrapper.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.model.LinkProcessingError;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    void sendFailureReport_shouldGroupErrorsByChat() {
        LinkProcessingError error1 = LinkProcessingError.builder()
                .linkId(1L)
                .url("https://github.com/a/b")
                .reason("github api unavailable")
                .chatIds(List.of(100L, 200L))
                .build();

        LinkProcessingError error2 = LinkProcessingError.builder()
                .linkId(2L)
                .url("https://stackoverflow.com/questions/1")
                .reason("timeout")
                .chatIds(List.of(100L))
                .build();

        service.sendFailureReport(List.of(error1, error2));

        verify(botClient, times(2)).sendNotification(any());
    }
}
