package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.GitHubIssueResponse;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface GitHubClient {

    @GetExchange("/repos/{owner}/{repo}/issues")
    List<GitHubIssueResponse> fetchRepositoryIssues(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam("state") String state,
            @RequestParam("sort") String sort,
            @RequestParam("direction") String direction,
            @RequestParam("per_page") int perPage);
}
