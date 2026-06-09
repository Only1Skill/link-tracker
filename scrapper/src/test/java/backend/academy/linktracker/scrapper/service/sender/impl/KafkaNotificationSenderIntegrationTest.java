package backend.academy.linktracker.scrapper.service.sender.impl;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.properties.KafkaTopicProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
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
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class KafkaNotificationSenderIntegrationTest {

    private static final String TOPIC = "link-updates-test-" + UUID.randomUUID();
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);
    private static final Duration MAX_WAIT_TIME = Duration.ofSeconds(10);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void createTopic() throws Exception {
        Map<String, Object> adminProperties =
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(adminProperties)) {
            adminClient
                    .createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1)))
                    .all()
                    .get();
        }
    }

    @Test
    void sendUpdates_shouldSendLinkUpdatesToKafkaTopicAsJson() throws Exception {
        KafkaTemplate<Long, LinkUpdate> kafkaTemplate = createKafkaTemplate();

        KafkaTopicProperties topicProperties = new KafkaTopicProperties();
        topicProperties.setLinkUpdates(TOPIC);

        KafkaNotificationSender sender = new KafkaNotificationSender(kafkaTemplate, topicProperties);

        LinkUpdate firstUpdate =
                new LinkUpdate(1L, "https://github.com/test-owner/test-repo", "First update", List.of(100L, 200L));

        LinkUpdate secondUpdate = new LinkUpdate(
                2L, "https://stackoverflow.com/questions/12345/how-to-write-tests", "Second update", List.of(300L));

        try (KafkaConsumer<Long, String> consumer = createConsumer()) {
            consumer.subscribe(List.of(TOPIC));

            sender.sendUpdates(List.of(firstUpdate, secondUpdate));
            kafkaTemplate.flush();

            List<ConsumerRecord<Long, String>> records = pollRecords(consumer, 2);

            assertThat(records).hasSize(2);

            ConsumerRecord<Long, String> firstRecord = records.get(0);
            ConsumerRecord<Long, String> secondRecord = records.get(1);

            assertThat(firstRecord.key()).isEqualTo(1L);
            assertThat(secondRecord.key()).isEqualTo(2L);

            LinkUpdate firstConsumedUpdate = objectMapper.readValue(firstRecord.value(), LinkUpdate.class);
            LinkUpdate secondConsumedUpdate = objectMapper.readValue(secondRecord.value(), LinkUpdate.class);

            assertThat(firstConsumedUpdate).isEqualTo(firstUpdate);
            assertThat(secondConsumedUpdate).isEqualTo(secondUpdate);
        }
    }

    private KafkaTemplate<Long, LinkUpdate> createKafkaTemplate() {
        Map<String, Object> producerProperties = new HashMap<>();

        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        producerProperties.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        DefaultKafkaProducerFactory<Long, LinkUpdate> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProperties);

        return new KafkaTemplate<>(producerFactory);
    }

    private KafkaConsumer<Long, String> createConsumer() {
        Map<String, Object> consumerProperties = new HashMap<>();

        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-notification-sender-test-" + UUID.randomUUID());
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new KafkaConsumer<>(consumerProperties);
    }

    private List<ConsumerRecord<Long, String>> pollRecords(KafkaConsumer<Long, String> consumer, int expectedCount) {
        List<ConsumerRecord<Long, String>> records = new ArrayList<>();
        long deadline = System.nanoTime() + MAX_WAIT_TIME.toNanos();

        while (records.size() < expectedCount && System.nanoTime() < deadline) {
            ConsumerRecords<Long, String> polledRecords = consumer.poll(POLL_TIMEOUT);
            polledRecords.forEach(records::add);
        }

        return records;
    }
}
