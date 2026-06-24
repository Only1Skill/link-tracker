package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.impl.BotRestClient;
import backend.academy.linktracker.scrapper.properties.BotProperties;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfiguration {
    @Bean
    public GitHubClient gitHubClient(GithubProperties githubProperties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(githubProperties.getBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(GitHubClient.class);
    }

    @Bean
    public StackOverflowClient stackOverflowClient(StackoverflowProperties stackoverflowProperties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(stackoverflowProperties.getBaseUrl())
                .defaultHeader("Accept", "application/json")
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(StackOverflowClient.class);
    }

    @Bean
    public RestClient restClientForBot(BotProperties botProperties) {
        return RestClient.builder().baseUrl(botProperties.getBaseUrl()).build();
    }

    @Bean
    public BotClient botClient(RestClient restClientForBot) {
        return new BotRestClient(restClientForBot);
    }
}
