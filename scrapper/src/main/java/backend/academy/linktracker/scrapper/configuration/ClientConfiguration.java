package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.impl.BotRestClient;
import backend.academy.linktracker.scrapper.client.impl.ResilientBotClient;
import backend.academy.linktracker.scrapper.client.impl.ResilientGitHubClient;
import backend.academy.linktracker.scrapper.client.impl.ResilientStackOverflowClient;
import backend.academy.linktracker.scrapper.properties.BotProperties;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import backend.academy.linktracker.scrapper.resilience.HttpResilienceExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfiguration {
    @Bean
    public GitHubClient gitHubClient(
            GithubProperties githubProperties,
            ClientHttpRequestFactory clientHttpRequestFactory,
            HttpResilienceExecutor executor) {
        RestClient restClient = RestClient.builder()
                .baseUrl(githubProperties.getBaseUrl())
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + githubProperties.getToken())
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();

        GitHubClient delegate = createClient(restClient, GitHubClient.class);
        return new ResilientGitHubClient(delegate, executor);
    }

    @Bean
    public StackOverflowClient stackOverflowClient(
            StackoverflowProperties stackoverflowProperties,
            ClientHttpRequestFactory clientHttpRequestFactory,
            HttpResilienceExecutor executor) {
        RestClient restClient = RestClient.builder()
                .baseUrl(stackoverflowProperties.getBaseUrl())
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader("Accept", "application/json")
                .build();
        StackOverflowClient delegate = createClient(restClient, StackOverflowClient.class);
        return new ResilientStackOverflowClient(delegate, executor);
    }

    @Bean
    public RestClient restClientForBot(BotProperties botProperties, ClientHttpRequestFactory clientHttpRequestFactory) {
        return RestClient.builder()
                .baseUrl(botProperties.getBaseUrl())
                .requestFactory(clientHttpRequestFactory)
                .build();
    }

    @Bean
    public BotClient botClient(RestClient restClientForBot, HttpResilienceExecutor executor) {
        return new ResilientBotClient(new BotRestClient(restClientForBot), executor);
    }

    private static <T> T createClient(RestClient restClient, Class<T> clientClass) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(clientClass);
    }
}
