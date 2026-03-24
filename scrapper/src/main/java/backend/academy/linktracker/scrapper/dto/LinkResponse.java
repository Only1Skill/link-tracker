package backend.academy.linktracker.scrapper.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

public record LinkResponse(Long id, URI url, List<String> tags, OffsetDateTime lastUpdate) {

    @SuppressFBWarnings("EI_EXPOSE_REP")
    @Override
    public List<String> tags() {
        return tags;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public LinkResponse {}
}
