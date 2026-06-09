package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StackOverflowOwnerResponse(
        @JsonProperty("display_name") String displayName) {}
