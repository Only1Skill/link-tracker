package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StackOverflowQuestionResponse(
        @JsonProperty("question_id") Long questionId,
        @JsonProperty("title") String title,
        @JsonProperty("body") String body,
        @JsonProperty("creation_date") Long creationDate,
        @JsonProperty("last_activity_date") Long lastActivityDate,
        @JsonProperty("link") String link,
        @JsonProperty("owner") StackOverflowOwnerResponse owner) {}
