package backend.academy.linktracker.scrapper.client.impl;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.dto.GitHubIssueResponse;
import backend.academy.linktracker.scrapper.resilience.HttpResilienceExecutor;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResilientGitHubClient implements GitHubClient {

    private static final String CLIENT_NAME = "github";

    private final GitHubClient delegate;
    private final HttpResilienceExecutor executor;

    @Override
    public List<GitHubIssueResponse> fetchRepositoryIssues(
            String owner, String repo, String state, String sort, String direction, int perPage) {
        return executor.execute(
                CLIENT_NAME, () -> delegate.fetchRepositoryIssues(owner, repo, state, sort, direction, perPage));
    }
}
