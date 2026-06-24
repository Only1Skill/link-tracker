package backend.academy.linktracker.scrapper.properties;

import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.cache.links")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class LinkListCacheProperties {

    private boolean enabled = true;

    @DurationMin(seconds = 1)
    private Duration ttl = Duration.ofMinutes(10);
}
