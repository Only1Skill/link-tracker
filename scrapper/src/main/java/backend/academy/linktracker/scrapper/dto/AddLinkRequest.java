package backend.academy.linktracker.scrapper.dto;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.internal.annotation.SuppressFBWarnings;

@Getter
@Setter
@RequiredArgsConstructor
public class AddLinkRequest {
    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    private String link;

    private List<String> tags;
}
