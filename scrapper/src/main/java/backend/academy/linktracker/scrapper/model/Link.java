package backend.academy.linktracker.scrapper.model;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Link {
    private Long id;
    private Long chatId;
    private String url;
    private List<String> tags;
    private OffsetDateTime lastCheckTime;
    private OffsetDateTime lastUpdateTime;
}
