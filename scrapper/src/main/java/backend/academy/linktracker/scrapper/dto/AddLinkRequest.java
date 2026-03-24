package backend.academy.linktracker.scrapper.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.hibernate.validator.constraints.URL;

public record AddLinkRequest(
        @NotBlank(message = "Ссылка не может быть пустой") @URL(message = "Ссылка должна быть корректным URL")
        String link,

        List<String> tags) {
    @SuppressFBWarnings("EI_EXPOSE_REP")
    @Override
    public List<String> tags() {
        return tags;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AddLinkRequest {}
}
