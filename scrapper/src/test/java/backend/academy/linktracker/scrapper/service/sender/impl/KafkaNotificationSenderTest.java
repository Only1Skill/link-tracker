package backend.academy.linktracker.scrapper.service.sender.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.properties.KafkaTopicProperties;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaNotificationSenderTest {

    private static final String LINK_UPDATES_TOPIC = "link-updates";

    @Mock
    private KafkaTemplate<Long, LinkUpdate> kafkaTemplate;

    private KafkaNotificationSender sender;

    @BeforeEach
    void setUp() {
        KafkaTopicProperties topicProperties = new KafkaTopicProperties();
        topicProperties.setLinkUpdates(LINK_UPDATES_TOPIC);

        sender = new KafkaNotificationSender(kafkaTemplate, topicProperties);
    }

    @Test
    void sendUpdates_shouldSendEveryUpdateToKafkaTopic() {
        LinkUpdate firstUpdate =
                new LinkUpdate(1L, "https://github.com/test-owner/test-repo", "First update", List.of(100L, 200L));

        LinkUpdate secondUpdate = new LinkUpdate(
                2L, "https://stackoverflow.com/questions/12345/how-to-write-tests", "Second update", List.of(300L));

        when(kafkaTemplate.send(LINK_UPDATES_TOPIC, 1L, firstUpdate))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaTemplate.send(LINK_UPDATES_TOPIC, 2L, secondUpdate))
                .thenReturn(CompletableFuture.completedFuture(null));

        sender.sendUpdates(List.of(firstUpdate, secondUpdate));

        verify(kafkaTemplate).send(LINK_UPDATES_TOPIC, 1L, firstUpdate);
        verify(kafkaTemplate).send(LINK_UPDATES_TOPIC, 2L, secondUpdate);
    }

    @Test
    void sendUpdates_shouldDoNothing_whenUpdatesAreNull() {
        sender.sendUpdates(null);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void sendUpdates_shouldDoNothing_whenUpdatesAreEmpty() {
        sender.sendUpdates(List.of());

        verifyNoInteractions(kafkaTemplate);
    }
}
