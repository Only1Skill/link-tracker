package backend.academy.linktracker.scrapper.service.sender.impl;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FallbackNotificationSenderTest {

    @Mock
    private HttpNotificationSender httpNotificationSender;

    @Mock
    private KafkaNotificationSender kafkaNotificationSender;

    private FallbackNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new FallbackNotificationSender(httpNotificationSender, kafkaNotificationSender);
    }

    @Test
    void sendUpdates_shouldUseHttpTransportWhenItIsAvailable() {
        List<LinkUpdate> updates = updates();

        sender.sendUpdates(updates);

        verify(httpNotificationSender).sendUpdates(updates);
        verifyNoInteractions(kafkaNotificationSender);
    }

    @Test
    void sendUpdates_shouldFallbackToKafkaWhenHttpTransportFails() {
        List<LinkUpdate> updates = updates();
        doThrow(new IllegalStateException("bot is unavailable"))
                .when(httpNotificationSender)
                .sendUpdates(updates);

        sender.sendUpdates(updates);

        verify(httpNotificationSender).sendUpdates(updates);
        verify(kafkaNotificationSender).sendUpdates(updates);
    }

    private static List<LinkUpdate> updates() {
        return List.of(new LinkUpdate(1L, "https://github.com/owner/repo", "update", List.of(10L)));
    }
}
