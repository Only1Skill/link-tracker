package backend.academy.linktracker.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.hibernate.validator.constraints.URL;

public record AddLinkRequest(
        @NotBlank(message = "Ссылка не может быть пустой") @URL(message = "Ссылка должна быть корректным URL")
        String link,

        List<String> tags) {}
