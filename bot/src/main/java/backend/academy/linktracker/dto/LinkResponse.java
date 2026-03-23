package backend.academy.linktracker.dto;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

public record LinkResponse(Long id, URI url, List<String> tags, OffsetDateTime lastUpdate) {}
