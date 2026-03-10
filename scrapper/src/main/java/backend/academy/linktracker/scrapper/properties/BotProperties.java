package backend.academy.linktracker.scrapper.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bot")
@Getter
@Setter
public class BotProperties {
    private String baseUrl;
}
