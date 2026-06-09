package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Min;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.notification")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class NotificationProperties {

    public static final int DEFAULT_PREVIEW_LENGTH = 200;

    @Min(1)
    private int previewLength = DEFAULT_PREVIEW_LENGTH;
}
