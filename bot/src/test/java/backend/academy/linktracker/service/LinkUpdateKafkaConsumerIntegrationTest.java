package backend.academy.linktracker.service;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.dto.LinkUpdate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
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
            "app.kafka.enabled=true",
            "app.kafka.topic.link-updates=link-updates-test",
            "app.kafka.topic.link-updates-dlq=link-updates-dlq-test",
            "app.kafka.topic.partitions=1",
            "app.kafka.topic.replication-factor=1",
            "app.kafka.topic.min-in-sync-replicas=1",
            "app.kafka.consumer.retry-attempts=0",
            "app.kafka.consumer.retry-interval=100ms",
            "spring.kafka.admin.auto-create=false",
            "spring.kafka.consumer.group-id=bot-link-updates-test",
            "spring.kafka.consumer.auto-offset-reset=earliest",
            "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.LongDeserializer",
            "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JacksonJsonDeserializer",
            "spring.kafka.consumer.properties.spring.json.value.default.type=backend.academy.linktracker.dto.LinkUpdate",
            "spring.kafka.consumer.properties.spring.json.trusted.packages=backend.academy.linktracker.dto",
            "app.telegram.token=test-token",
            "app.telegram.debug=true"
        })
class LinkUpdateKafkaConsumerIntegrationTest {

    private static final String TOPIC = "link-updates-test";

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @BeforeAll
    static void createTopic() throws Exception {
        Map<String, Object> adminProperties = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(adminProperties)) {
            adminClient.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1)))
                    .all()
                    .get();
        }
    }

    @MockitoBean
    private TelegramClient telegramClient;

    @Test
    void shouldConsumeLinkUpdateFromKafkaAndSendTelegramMessages() {
        LinkUpdate update = new LinkUpdate(
                1L,
                "https://github.com/test-owner/test-repo",
                "New issue title",
                List.of(100L, 200L));

        KafkaTemplate<Long, LinkUpdate> kafkaTemplate = createKafkaTemplate();

        try {
            kafkaTemplate.send(TOPIC, update.id(), update);
            kafkaTemplate.flush();
        } finally {
            kafkaTemplate.destroy();
        }

        verify(telegramClient, timeout(10_000)).sendMessage(100L, "New issue title");
        verify(telegramClient, timeout(10_000)).sendMessage(200L, "New issue title");
    }

    private KafkaTemplate<Long, LinkUpdate> createKafkaTemplate() {
        Map<String, Object> producerProperties = new HashMap<>();

        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        producerProperties.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProperties));
    }
}
