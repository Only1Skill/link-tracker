package backend.academy.linktracker.scrapper.client.dto;

import java.util.List;

public record StackOverflowApiResponse<T>(List<T> items) {
    public StackOverflowApiResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
