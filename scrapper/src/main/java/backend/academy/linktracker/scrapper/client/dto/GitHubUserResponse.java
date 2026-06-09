package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubUserResponse(@JsonProperty("login") String login) {}
