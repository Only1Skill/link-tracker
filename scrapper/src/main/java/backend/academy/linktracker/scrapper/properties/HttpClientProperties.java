package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.resilience.timeout")
@Validated
@Getter
@Setter
public class HttpClientProperties {

    @NotNull
    @DurationMin(millis = 1)
    private Duration connectTimeout = Duration.ofSeconds(1);

    @NotNull
    @DurationMin(millis = 1)
    private Duration readTimeout = Duration.ofSeconds(2);
}
