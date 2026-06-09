package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.github")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class GithubProperties {
    @NotEmpty
    private String baseUrl;

    @NotEmpty
    private String token;

    @Min(1)
    private int perPage;

    @NotEmpty
    private String issuesState;

    @NotEmpty
    private String issuesSort;

    @NotEmpty
    private String issuesDirection;
}
