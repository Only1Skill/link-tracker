package backend.academy.linktracker.scrapper.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record LinkUpdate(
        @NotNull Long id,
        @NotNull String url,
        @NotEmpty String description,
        @NotEmpty List<@NotNull Long> tgChatIds) {

    @SuppressFBWarnings("EI_EXPOSE_REP")
    @Override
    public List<Long> tgChatIds() {
        return tgChatIds;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public LinkUpdate {}
}
