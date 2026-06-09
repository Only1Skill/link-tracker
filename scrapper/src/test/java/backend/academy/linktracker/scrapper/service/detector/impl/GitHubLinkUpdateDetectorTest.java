package backend.academy.linktracker.scrapper.service.detector.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.dto.GitHubIssueResponse;
import backend.academy.linktracker.scrapper.client.dto.GitHubPullRequestMarkerResponse;
import backend.academy.linktracker.scrapper.client.dto.GitHubUserResponse;
import backend.academy.linktracker.scrapper.model.LinkEvent;
import backend.academy.linktracker.scrapper.model.LinkEventType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GitHubLinkUpdateDetectorTest {

    @Mock
    private GitHubClient gitHubClient;

    private GitHubLinkUpdateDetector detector;
    private GithubProperties githubProperties;

    @BeforeEach
    void setUp() {
        githubProperties = new GithubProperties();
        githubProperties.setBaseUrl("https://api.github.com");
        githubProperties.setToken("token");
        githubProperties.setPerPage(100);
        githubProperties.setIssuesState("all");
        githubProperties.setIssuesSort("updated");
        githubProperties.setIssuesDirection("asc");

        detector = new GitHubLinkUpdateDetector(gitHubClient, githubProperties);
    }

    @Test
    void supports_shouldReturnTrue_forGithubUrl() {
        assertThat(detector.supports("https://github.com/test-owner/test-repo")).isTrue();
    }

    @Test
    void supports_shouldReturnFalse_forUnsupportedUrl() {
        assertThat(detector.supports("https://stackoverflow.com/questions/123/test"))
                .isFalse();
    }

    @Test
    void detectUpdates_shouldMapIssueAndPr_filterOldOnes_andSortByCreatedAt() {
        OffsetDateTime lastCheckTime = OffsetDateTime.parse("2026-04-16T10:00:00Z");
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://github.com/test-owner/test-repo")
                .lastCheckTime(lastCheckTime)
                .lastUpdateTime(lastCheckTime)
                .build();

        GitHubIssueResponse oldIssue = new GitHubIssueResponse(
                1L,
                "Old issue",
                "Old body",
                OffsetDateTime.parse("2026-04-16T09:00:00Z"),
                OffsetDateTime.parse("2026-04-16T09:00:00Z"),
                "https://github.com/test-owner/test-repo/issues/1",
                new GitHubUserResponse("old-user"),
                null);

        GitHubIssueResponse newPr = new GitHubIssueResponse(
                2L,
                "New PR",
                "PR body",
                OffsetDateTime.parse("2026-04-16T10:20:00Z"),
                OffsetDateTime.parse("2026-04-16T10:20:00Z"),
                "https://github.com/test-owner/test-repo/pull/2",
                new GitHubUserResponse("pr-user"),
                new GitHubPullRequestMarkerResponse("https://github.com/test-owner/test-repo/pull/2"));

        GitHubIssueResponse newIssue = new GitHubIssueResponse(
                3L,
                "New issue",
                "Issue body",
                OffsetDateTime.parse("2026-04-16T10:10:00Z"),
                OffsetDateTime.parse("2026-04-16T10:10:00Z"),
                "https://github.com/test-owner/test-repo/issues/3",
                new GitHubUserResponse("issue-user"),
                null);

        when(gitHubClient.fetchRepositoryIssues("test-owner", "test-repo", "all", "updated", "asc", 100))
                .thenReturn(List.of(newPr, oldIssue, newIssue));

        List<LinkEvent> events = detector.detectUpdates(trackedLink);

        assertThat(events).hasSize(2);
        assertThat(events.getFirst().getType()).isEqualTo(LinkEventType.GITHUB_ISSUE);
        assertThat(events.getFirst().getTitle()).isEqualTo("New issue");
        assertThat(events.getFirst().getAuthor()).isEqualTo("issue-user");

        assertThat(events.get(1).getType()).isEqualTo(LinkEventType.GITHUB_PR);
        assertThat(events.get(1).getTitle()).isEqualTo("New PR");
        assertThat(events.get(1).getAuthor()).isEqualTo("pr-user");

        verify(gitHubClient).fetchRepositoryIssues("test-owner", "test-repo", "all", "updated", "asc", 100);
    }

    @Test
    void detectUpdates_shouldThrow_whenUrlIsUnsupported() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(1L)
                .url("https://example.com/not-supported")
                .lastCheckTime(OffsetDateTime.now())
                .lastUpdateTime(OffsetDateTime.now())
                .build();

        assertThatThrownBy(() -> detector.detectUpdates(trackedLink))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не поддерживает");
    }
}
