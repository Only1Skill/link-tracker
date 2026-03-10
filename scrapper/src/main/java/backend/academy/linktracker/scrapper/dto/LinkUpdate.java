package backend.academy.linktracker.scrapper.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record LinkUpdate(
    @NotNull
    Long id,
    @NotNull
    String url,
    @NotEmpty
    String description,
    @NotEmpty
    List<@NotNull Long> tgChatIds
) {
}
