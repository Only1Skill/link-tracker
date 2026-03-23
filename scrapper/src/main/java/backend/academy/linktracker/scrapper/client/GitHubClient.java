package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.GitHubRepositoryResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface GitHubClient {

    @GetExchange("/repos/{owner}/{repo}")
    GitHubRepositoryResponse fetchRepository(@PathVariable String owner, @PathVariable String repo);
}
