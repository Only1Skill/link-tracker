package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubPullRequestMarkerResponse(
        @JsonProperty("html_url") String htmlUrl) {}
