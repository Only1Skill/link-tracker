package backend.academy.linktracker.scrapper.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LinkProcessingError {
    private final Long linkId;
    private final String url;
    private final String reason;
    private final List<Long> chatIds;
}
