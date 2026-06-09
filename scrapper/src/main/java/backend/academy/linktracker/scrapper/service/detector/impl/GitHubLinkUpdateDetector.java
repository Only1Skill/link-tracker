package backend.academy.linktracker.scrapper.service.detector.impl;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.dto.GitHubIssueResponse;
import backend.academy.linktracker.scrapper.client.dto.GitHubUserResponse;
import backend.academy.linktracker.scrapper.model.LinkEvent;
import backend.academy.linktracker.scrapper.model.LinkEventType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.service.detector.LinkUpdateDetector;
import backend.academy.linktracker.scrapper.util.LinkParser;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GitHubLinkUpdateDetector implements LinkUpdateDetector {

    private final GitHubClient gitHubClient;
    private final GithubProperties githubProperties;

    @Override
    public boolean supports(String url) {
        return LinkParser.parseGitHub(url) != null;
    }

    @Override
    public List<LinkEvent> detectUpdates(TrackedLink trackedLink) {
        LinkParser.GitHubData githubData = LinkParser.parseGitHub(trackedLink.getUrl());
        if (githubData == null) {
            throw new IllegalArgumentException("GitHub не поддерживает данную ссылку: " + trackedLink.getUrl());
        }

        OffsetDateTime baseline = trackedLink.getLastUpdateTime();

        List<GitHubIssueResponse> issues = gitHubClient.fetchRepositoryIssues(
                githubData.owner(),
                githubData.repo(),
                githubProperties.getIssuesState(),
                githubProperties.getIssuesSort(),
                githubProperties.getIssuesDirection(),
                githubProperties.getPerPage());

        return issues.stream()
                .filter(issue -> issue.createdAt() != null)
                .filter(issue -> issue.createdAt().toInstant().isAfter(baseline.toInstant()))
                .map(issue -> mapToEvent(trackedLink, issue))
                .sorted(Comparator.comparing(LinkEvent::getCreatedAt))
                .toList();
    }

    private LinkEvent mapToEvent(TrackedLink trackedLink, GitHubIssueResponse issue) {
        return LinkEvent.builder()
                .linkId(trackedLink.getId())
                .url(trackedLink.getUrl())
                .type(issue.isPullRequest() ? LinkEventType.GITHUB_PR : LinkEventType.GITHUB_ISSUE)
                .title(safe(issue.title()))
                .author(resolveAuthor(issue.user()))
                .createdAt(issue.createdAt())
                .content(safe(issue.body()))
                .eventUrl(safe(issue.htmlUrl()))
                .build();
    }

    private String resolveAuthor(GitHubUserResponse user) {
        return user == null || user.login() == null || user.login().isBlank() ? "unknown" : user.login();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
