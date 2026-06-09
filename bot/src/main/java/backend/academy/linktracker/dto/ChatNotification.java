package backend.academy.linktracker.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ChatNotification(
        @NotEmpty String message, @NotEmpty List<@NotNull Long> tgChatIds) {
    public ChatNotification {
        tgChatIds = List.copyOf(tgChatIds);
    }
}
