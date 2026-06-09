import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.configuration.KafkaConsumerConfiguration;
import backend.academy.linktracker.configuration.KafkaConsumerProperties;
import backend.academy.linktracker.configuration.KafkaProducerConfiguration;
import backend.academy.linktracker.configuration.KafkaTopicConfiguration;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.service.sender.impl.KafkaNotificationSender;
import backend.academy.linktracker.service.LinkUpdateKafkaConsumer;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
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
        classes = ScrapperToBotKafkaIntegrationTest.TestBotKafkaApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "app.kafka.enabled=true",
            "app.kafka.consumer.retry-attempts=0",
            "app.kafka.consumer.retry-interval=100ms",
            "app.kafka.topic.partitions=1",
            "app.kafka.topic.replication-factor=1",
            "app.kafka.topic.min-in-sync-replicas=1"
        })
class ScrapperToBotKafkaIntegrationTest {

    private static final String LINK_UPDATES_TOPIC = "scrapper-to-bot-e2e-link-updates-" + UUID.randomUUID();
    private static final String LINK_UPDATES_DLQ_TOPIC = "scrapper-to-bot-e2e-link-updates-dlq-" + UUID.randomUUID();

    private static final Long LINK_ID = 1L;
    private static final Long CHAT_ID = 5686547533L;
    private static final String URL = "https://github.com/test-owner/test-repo";
    private static final String DESCRIPTION = "E2E Kafka notification from Scrapper to Bot";

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.topic.link-updates", () -> LINK_UPDATES_TOPIC);
        registry.add("app.kafka.topic.link-updates-dlq", () -> LINK_UPDATES_DLQ_TOPIC);
    }

    @MockitoBean
    private TelegramClient telegramClient;

    @BeforeEach
    void setUp() {
        reset(telegramClient);
    }

    @Test
    void shouldDeliverNotificationFromScrapperToBotThroughKafka() {
        LinkUpdate update = new LinkUpdate(LINK_ID, URL, DESCRIPTION, List.of(CHAT_ID));

        sendFromScrapper(update);

        verify(telegramClient, timeout(15_000)).sendMessage(CHAT_ID, DESCRIPTION);
    }

    private void sendFromScrapper(LinkUpdate update) {
        Map<String, Object> producerProperties = new HashMap<>();

        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        producerProperties.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        DefaultKafkaProducerFactory<Long, LinkUpdate> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProperties);

        KafkaTemplate<Long, LinkUpdate> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        backend.academy.linktracker.scrapper.properties.KafkaTopicProperties topicProperties =
                new backend.academy.linktracker.scrapper.properties.KafkaTopicProperties();
        topicProperties.setLinkUpdates(LINK_UPDATES_TOPIC);

        KafkaNotificationSender sender = new KafkaNotificationSender(kafkaTemplate, topicProperties);

        try {
            sender.sendUpdates(List.of(update));
            kafkaTemplate.flush();
        } finally {
            producerFactory.destroy();
        }
    }

    @SpringBootConfiguration
    @EnableKafka
    @EnableConfigurationProperties({
        backend.academy.linktracker.configuration.KafkaTopicProperties.class,
        KafkaConsumerProperties.class
    })
    @Import({
        LinkUpdateKafkaConsumer.class,
        KafkaConsumerConfiguration.class,
        KafkaProducerConfiguration.class,
        KafkaTopicConfiguration.class
    })
    static class TestBotKafkaApplication {

        @Bean
        ConsumerFactory<Long, backend.academy.linktracker.dto.LinkUpdate> consumerFactory(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            Map<String, Object> consumerProperties = new HashMap<>();

            consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "scrapper-to-bot-e2e-test");
            consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
            consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
            consumerProperties.put("spring.deserializer.key.delegate.class", LongDeserializer.class);
            consumerProperties.put("spring.deserializer.value.delegate.class", JacksonJsonDeserializer.class);
            consumerProperties.put("spring.json.value.default.type", "backend.academy.linktracker.dto.LinkUpdate");
            consumerProperties.put("spring.json.trusted.packages", "backend.academy.linktracker.dto");

            return new DefaultKafkaConsumerFactory<>(consumerProperties);
        }

        @Bean
        Validator validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }
    }
}
