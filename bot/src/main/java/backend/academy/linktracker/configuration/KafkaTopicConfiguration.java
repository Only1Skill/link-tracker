package backend.academy.linktracker.configuration;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfiguration {

    private final KafkaTopicProperties topicProperties;

    @Bean
    public KafkaAdmin kafkaAdmin(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        KafkaAdmin kafkaAdmin = new KafkaAdmin(configs);
        kafkaAdmin.setFatalIfBrokerNotAvailable(false);

        return kafkaAdmin;
    }

    @Bean
    public KafkaAdmin.NewTopics linkTrackerKafkaTopics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(topicProperties.getLinkUpdates())
                        .partitions(topicProperties.getPartitions())
                        .replicas(topicProperties.getReplicationFactor())
                        .config(
                                TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
                                String.valueOf(topicProperties.getMinInSyncReplicas()))
                        .build(),
                TopicBuilder.name(topicProperties.getLinkUpdatesDlq())
                        .partitions(topicProperties.getPartitions())
                        .replicas(topicProperties.getReplicationFactor())
                        .config(
                                TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
                                String.valueOf(topicProperties.getMinInSyncReplicas()))
                        .build());
    }
}
