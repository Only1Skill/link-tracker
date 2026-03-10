package backend.academy.linktracker.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scrapper")
@Getter
@Setter
public class ScrapperProperties {
    private String baseUrl;
}
