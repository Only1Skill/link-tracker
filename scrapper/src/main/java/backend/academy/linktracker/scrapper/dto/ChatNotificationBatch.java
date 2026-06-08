package backend.academy.linktracker.scrapper.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ChatNotificationBatch(@NotEmpty List<@Valid @NotNull ChatNotification> notifications) {
    public ChatNotificationBatch {
        notifications = notifications == null ? List.of() : List.copyOf(notifications);
    }
}
