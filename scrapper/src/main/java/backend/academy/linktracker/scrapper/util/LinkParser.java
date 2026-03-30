package backend.academy.linktracker.scrapper.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class LinkParser {
    private static final Pattern GITHUB_PATTERN =
            Pattern.compile("^https?://github\\.com/(?<owner>[^/]+)/(?<repo>[^/]+).*$");
    private static final Pattern STACKOVERFLOW_PATTERN =
            Pattern.compile("^https?://stackoverflow\\.com/questions/(?<questionId>\\d+).*$");

    public static GitHubData parseGitHub(String url) {
        Matcher matcher = GITHUB_PATTERN.matcher(url);
        if (matcher.matches()) {
            return new GitHubData(matcher.group("owner"), matcher.group("repo"));
        }
        return null;
    }

    public static StackOverflowData parseStackOverflow(String url) {
        Matcher matcher = STACKOVERFLOW_PATTERN.matcher(url);
        if (matcher.matches()) {
            return new StackOverflowData(matcher.group("questionId"));
        }
        return null;
    }

    public record GitHubData(String owner, String repo) {}

    public record StackOverflowData(String questionId) {}
}
