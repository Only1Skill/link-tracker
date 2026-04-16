package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record GitHubIssueResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("title") String title,
        @JsonProperty("body") String body,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("user") GitHubUserResponse user,
        @JsonProperty("pull_request") GitHubPullRequestMarkerResponse pullRequest) {
    public boolean isPullRequest() {
        return pullRequest != null;
    }
}
