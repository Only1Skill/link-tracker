package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.rate-limiting")
@Validated
@Getter
@Setter
public class RateLimitingProperties {

    private boolean enabled = true;

    @Min(1)
    private int limitForPeriod = 60;

    @NotNull
    @DurationMin(millis = 1)
    private Duration limitRefreshPeriod = Duration.ofMinutes(1);
}
