package backend.academy.linktracker.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.kafka.topic")
@Validated
@Getter
@Setter
public class KafkaTopicProperties {

    @NotBlank
    private String linkUpdates = "link-updates";

    @NotBlank
    private String linkUpdatesDlq = "link-updates-dlq";

    @Min(1)
    private int partitions = 3;

    @Min(1)
    private int replicationFactor = 1;

    @Min(1)
    private int minInSyncReplicas = 1;

    @AssertTrue(message = "min-in-sync-replicas не может быть больше replication-factor")
    public boolean isReplicationConfigurationValid() {
        return minInSyncReplicas <= replicationFactor;
    }
}
