package backend.academy.linktracker.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.resilience.retry")
@Validated
@Getter
@Setter
public class RetryProperties {

    @Min(1)
    private int maxAttempts = 3;

    @NotNull
    @DurationMin(millis = 1)
    private Duration waitDuration = Duration.ofMillis(200);

    private Set<Integer> retryableStatuses = new LinkedHashSet<>(Set.of(500, 502, 503, 504));
}
