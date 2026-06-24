package backend.academy.linktracker.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.resilience.circuit-breaker")
@Validated
@Getter
@Setter
public class CircuitBreakerProperties {

    @Min(1)
    private int slidingWindowSize = 10;

    @Min(1)
    private int minimumNumberOfCalls = 5;

    @Min(1)
    @Max(100)
    private float failureRateThreshold = 100;

    @Min(1)
    private int permittedNumberOfCallsInHalfOpenState = 5;

    @NotNull
    @DurationMin(millis = 1)
    private Duration waitDurationInOpenState = Duration.ofSeconds(1);
}
