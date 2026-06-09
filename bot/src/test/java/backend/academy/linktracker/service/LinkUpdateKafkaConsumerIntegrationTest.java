package backend.academy.linktracker.service;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.dto.LinkUpdate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(
        properties = {
            "app.kafka.topic.link-updates=link-updates-test",
            "spring.kafka.consumer.group-id=bot-link-updates-test",
            "spring.kafka.consumer.auto-offset-reset=earliest",
            "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.LongDeserializer",
            "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JacksonJsonDeserializer",
            "spring.kafka.consumer.properties.spring.json.value.default.type=backend.academy.linktracker.dto.LinkUpdate",
            "spring.kafka.consumer.properties.spring.json.trusted.packages=backend.academy.linktracker.dto",
            "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.LongSerializer",
            "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer",
            "spring.kafka.producer.properties.spring.json.add.type.headers=false",
            "app.telegram.token=test-token",
            "app.telegram.debug=true"
        })
class LinkUpdateKafkaConsumerIntegrationTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<Long, LinkUpdate> kafkaTemplate;

    @MockitoBean
    private TelegramClient telegramClient;

    @Test
    void shouldConsumeLinkUpdateFromKafkaAndSendTelegramMessages() {
        LinkUpdate update =
                new LinkUpdate(1L, "https://github.com/test-owner/test-repo", "New issue title", List.of(100L, 200L));

        kafkaTemplate.send("link-updates-test", update.id(), update);
        kafkaTemplate.flush();

        verify(telegramClient, timeout(10_000)).sendMessage(100L, "New issue title");
        verify(telegramClient, timeout(10_000)).sendMessage(200L, "New issue title");
    }
}
