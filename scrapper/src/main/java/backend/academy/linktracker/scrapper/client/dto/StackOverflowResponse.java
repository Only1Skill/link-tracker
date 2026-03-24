package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

public record StackOverflowResponse(List<StackOverflowQuestions> items) {

    @SuppressFBWarnings("EI_EXPOSE_REP")
    @Override
    public List<StackOverflowQuestions> items() {
        return items;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public StackOverflowResponse {}

    public record StackOverflowQuestions(
            @JsonProperty("question_id") Long questionId,
            @JsonProperty("last_activity_date") Long lastActivityDate) {}
}
