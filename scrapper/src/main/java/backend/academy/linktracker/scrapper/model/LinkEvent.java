package backend.academy.linktracker.scrapper.model;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LinkEvent {
    private Long linkId;
    private String url;
    private LinkEventType type;
    private String title;
    private String author;
    private OffsetDateTime createdAt;
    private String content;
    private String eventUrl;
}
