package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StackOverflowCommentResponse(
        @JsonProperty("comment_id") Long commentId,
        @JsonProperty("post_id") Long postId,
        @JsonProperty("body") String body,
        @JsonProperty("creation_date") Long creationDate,
        @JsonProperty("link") String link,
        @JsonProperty("owner") StackOverflowOwnerResponse owner) {}
