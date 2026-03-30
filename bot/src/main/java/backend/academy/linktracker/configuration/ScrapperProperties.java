package backend.academy.linktracker.configuration;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.scrapper")
@Getter
@Setter
@Validated
public class ScrapperProperties {
    @NotEmpty
    @URL
    private String baseUrl;
}
