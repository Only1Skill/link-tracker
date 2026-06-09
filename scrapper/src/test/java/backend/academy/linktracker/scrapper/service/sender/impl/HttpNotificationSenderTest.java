package backend.academy.linktracker.scrapper.service.sender.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.LinkUpdateBatch;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpNotificationSenderTest {

    @Mock
    private BotClient botClient;

    private HttpNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new HttpNotificationSender(botClient);
    }

    @Test
    void sendUpdates_shouldSendBatchThroughBotClient() {
        LinkUpdate firstUpdate =
                new LinkUpdate(1L, "https://github.com/test-owner/test-repo", "First update", List.of(100L, 200L));

        LinkUpdate secondUpdate = new LinkUpdate(
                2L, "https://stackoverflow.com/questions/12345/how-to-write-tests", "Second update", List.of(300L));

        sender.sendUpdates(List.of(firstUpdate, secondUpdate));

        ArgumentCaptor<LinkUpdateBatch> captor = ArgumentCaptor.forClass(LinkUpdateBatch.class);

        verify(botClient).sendUpdates(captor.capture());

        LinkUpdateBatch batch = captor.getValue();

        assertThat(batch.updates()).containsExactly(firstUpdate, secondUpdate);
    }

    @Test
    void sendUpdates_shouldDoNothing_whenUpdatesAreNull() {
        sender.sendUpdates(null);

        verify(botClient, never()).sendUpdates(any());
    }

    @Test
    void sendUpdates_shouldDoNothing_whenUpdatesAreEmpty() {
        sender.sendUpdates(List.of());

        verify(botClient, never()).sendUpdates(any());
    }
}
