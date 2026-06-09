package backend.academy.linktracker.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.kafka.consumer")
@Validated
@Getter
@Setter
public class KafkaConsumerProperties {

    @Min(0)
    private long retryAttempts = 3;

    @NotNull
    private Duration retryInterval = Duration.ofSeconds(1);
}
