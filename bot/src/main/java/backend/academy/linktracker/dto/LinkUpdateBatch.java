package backend.academy.linktracker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record LinkUpdateBatch(
        @NotEmpty List<@Valid @NotNull LinkUpdate> updates
) {
    public LinkUpdateBatch {
        updates = updates == null ? List.of() : List.copyOf(updates);
    }
}
