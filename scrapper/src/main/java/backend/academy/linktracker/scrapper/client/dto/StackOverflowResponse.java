package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StackOverflowResponse(List<StackOverflowQuestions> items) {
    public record StackOverflowQuestions(
            @JsonProperty("question_id") Long questionId,
            @JsonProperty("last_activity_date") Long lastActivityDate) {}
}
