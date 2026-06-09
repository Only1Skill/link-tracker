package backend.academy.linktracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.dto.LinkUpdate;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
            "spring.kafka.consumer.group-id=bot-link-updates-dlq-test",
            "spring.kafka.consumer.auto-offset-reset=earliest",
            "spring.kafka.consumer.key-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
            "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
            "spring.kafka.consumer.properties.spring.deserializer.key.delegate.class=org.apache.kafka.common.serialization.LongDeserializer",
            "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JacksonJsonDeserializer",
            "spring.kafka.consumer.properties.spring.json.value.default.type=backend.academy.linktracker.dto.LinkUpdate",
            "spring.kafka.consumer.properties.spring.json.trusted.packages=backend.academy.linktracker.dto",
            "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.LongSerializer",
            "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer",
            "spring.kafka.producer.properties.spring.json.add.type.headers=false",
            "app.kafka.consumer.retry-attempts=2",
            "app.kafka.consumer.retry-interval=100ms",
            "app.telegram.token=test-token",
            "app.telegram.debug=true"
        })
class LinkUpdateKafkaConsumerDlqIntegrationTest {

    private static final String SOURCE_TOPIC = "link-updates-dlq-source-test";
    private static final String DLQ_TOPIC = "link-updates-dlq-target-test";
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);
    private static final Duration MAX_WAIT_TIME = Duration.ofSeconds(15);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.topic.link-updates", () -> SOURCE_TOPIC);
        registry.add("app.kafka.topic.link-updates-dlq", () -> DLQ_TOPIC);
    }

    @BeforeAll
    static void createTopics() throws Exception {
        Map<String, Object> adminProperties =
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(adminProperties)) {
            adminClient
                    .createTopics(
                            List.of(new NewTopic(SOURCE_TOPIC, 1, (short) 1), new NewTopic(DLQ_TOPIC, 1, (short) 1)))
                    .all()
                    .get();
        }
    }

    @MockitoBean
    private TelegramClient telegramClient;

    @BeforeEach
    void setUp() {
        reset(telegramClient);
    }

    @Test
    void shouldRetryAndSendMessageToDlq_whenTelegramClientFails() {
        LinkUpdate update =
                new LinkUpdate(1L, "https://github.com/test-owner/test-repo", "New issue title", List.of(100L));

        doThrow(new RuntimeException("telegram unavailable"))
                .when(telegramClient)
                .sendMessage(100L, "New issue title");

        sendLinkUpdate(update.id(), update);

        ConsumerRecord<Long, String> dlqRecord = pollDlqRecordByKey(1L);

        assertThat(dlqRecord.key()).isEqualTo(1L);
        assertThat(dlqRecord.value())
                .contains("https://github.com/test-owner/test-repo")
                .contains("New issue title")
                .contains("100");

        verify(telegramClient, timeout(10_000).times(3)).sendMessage(100L, "New issue title");
    }

    @Test
    void shouldSendMessageToDlqWithoutRetry_whenMessageIsInvalid() {
        LinkUpdate invalidUpdate = new LinkUpdate(null, "https://github.com/test-owner/invalid-repo", "", List.of());

        sendLinkUpdate(2L, invalidUpdate);

        ConsumerRecord<Long, String> dlqRecord = pollDlqRecordByKey(2L);

        assertThat(dlqRecord.key()).isEqualTo(2L);
        assertThat(dlqRecord.value()).contains("https://github.com/test-owner/invalid-repo");

        verifyNoInteractions(telegramClient);
    }

    @Test
    void shouldSendMessageToDlqWithoutRetry_whenMessageCannotBeDeserialized() {
        sendRawMessage(3L, "{not-valid-json");

        ConsumerRecord<Long, String> dlqRecord = pollDlqRecordByKey(3L);

        assertThat(dlqRecord.key()).isEqualTo(3L);
        assertThat(dlqRecord.value()).isEqualTo("{not-valid-json");

        verifyNoInteractions(telegramClient);
    }

    private void sendLinkUpdate(Long key, LinkUpdate update) {
        Map<String, Object> producerProperties = new HashMap<>();

        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        producerProperties.put("spring.json.add.type.headers", false);

        DefaultKafkaProducerFactory<Long, LinkUpdate> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProperties);

        KafkaTemplate<Long, LinkUpdate> template = new KafkaTemplate<>(producerFactory);

        try {
            template.send(SOURCE_TOPIC, key, update);
            template.flush();
        } finally {
            producerFactory.destroy();
        }
    }

    private void sendRawMessage(Long key, String value) {
        Map<String, Object> producerProperties = new HashMap<>();

        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        try (KafkaProducer<Long, String> producer = new KafkaProducer<>(producerProperties)) {
            producer.send(new ProducerRecord<>(SOURCE_TOPIC, key, value));
            producer.flush();
        }
    }

    private ConsumerRecord<Long, String> pollDlqRecordByKey(Long expectedKey) {
        try (KafkaConsumer<Long, String> consumer = createDlqConsumer()) {
            consumer.subscribe(List.of(DLQ_TOPIC));

            long deadline = System.nanoTime() + MAX_WAIT_TIME.toNanos();

            while (System.nanoTime() < deadline) {
                ConsumerRecords<Long, String> records = consumer.poll(POLL_TIMEOUT);

                for (ConsumerRecord<Long, String> record : records) {
                    if (expectedKey.equals(record.key())) {
                        return record;
                    }
                }
            }
        }

        throw new AssertionError("Сообщение с key=" + expectedKey + " не найдено в DLQ topic " + DLQ_TOPIC);
    }

    private KafkaConsumer<Long, String> createDlqConsumer() {
        Map<String, Object> consumerProperties = new HashMap<>();

        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-test-consumer-" + UUID.randomUUID());
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new KafkaConsumer<>(consumerProperties);
    }
}
