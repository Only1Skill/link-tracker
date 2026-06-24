package backend.academy.linktracker.scrapper.model;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TrackedLink {
    private Long id;
    private String url;
    private OffsetDateTime lastCheckTime;
    private OffsetDateTime lastUpdateTime;
}
