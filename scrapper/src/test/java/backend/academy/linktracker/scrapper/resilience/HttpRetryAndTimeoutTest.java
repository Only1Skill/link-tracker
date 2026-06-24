package backend.academy.linktracker.scrapper.resilience;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.impl.BotRestClient;
import backend.academy.linktracker.scrapper.client.impl.ResilientBotClient;
import backend.academy.linktracker.scrapper.configuration.ResilienceConfiguration;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.LinkUpdateBatch;
import backend.academy.linktracker.scrapper.properties.CircuitBreakerProperties;
import backend.academy.linktracker.scrapper.properties.HttpClientProperties;
import backend.academy.linktracker.scrapper.properties.RetryProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class HttpRetryAndTimeoutTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final ResilienceConfiguration configuration = new ResilienceConfiguration();

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
    }

    @Test
    void sendUpdates_shouldRetryRetryableStatusWithConstantBackoff() {
        wireMock.stubFor(post(urlEqualTo("/updates/batch"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second"));
        wireMock.stubFor(post(urlEqualTo("/updates/batch"))
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("success"));
        wireMock.stubFor(post(urlEqualTo("/updates/batch"))
                .inScenario("retry")
                .whenScenarioStateIs("success")
                .willReturn(aResponse().withStatus(200)));

        BotClient client = resilientBotClient(retryProperties(3, Duration.ofMillis(50)), circuitBreakerProperties());

        long startedAt = System.nanoTime();
        client.sendUpdates(batch());
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        wireMock.verify(3, postRequestedFor(urlEqualTo("/updates/batch")));
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(100));
    }

    @Test
    void sendUpdates_shouldNotRetryNonRetryableStatus() {
        wireMock.stubFor(
                post(urlEqualTo("/updates/batch")).willReturn(aResponse().withStatus(400)));

        BotClient client = resilientBotClient(retryProperties(3, Duration.ofMillis(50)), circuitBreakerProperties());

        assertThatThrownBy(() -> client.sendUpdates(batch())).isInstanceOf(RestClientResponseException.class);

        wireMock.verify(1, postRequestedFor(urlEqualTo("/updates/batch")));
    }

    @Test
    void sendUpdates_shouldFailByTimeoutBeforeExternalServiceResponds() {
        wireMock.stubFor(post(urlEqualTo("/updates/batch"))
                .willReturn(aResponse().withFixedDelay(1_000).withStatus(200)));

        HttpClientProperties timeoutProperties = new HttpClientProperties();
        timeoutProperties.setConnectTimeout(Duration.ofMillis(100));
        timeoutProperties.setReadTimeout(Duration.ofMillis(100));
        ClientHttpRequestFactory requestFactory = configuration.clientHttpRequestFactory(timeoutProperties);
        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(requestFactory)
                .build();
        BotClient client = new BotRestClient(restClient);

        long startedAt = System.nanoTime();

        assertThatThrownBy(() -> client.sendUpdates(batch())).isInstanceOf(ResourceAccessException.class);

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        assertThat(elapsed).isLessThan(Duration.ofMillis(1_000));
    }

    private BotClient resilientBotClient(
            RetryProperties retryProperties, CircuitBreakerProperties circuitBreakerProperties) {
        HttpClientProperties timeoutProperties = new HttpClientProperties();
        ClientHttpRequestFactory requestFactory = configuration.clientHttpRequestFactory(timeoutProperties);
        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(requestFactory)
                .build();

        RetryRegistry retryRegistry = configuration.retryRegistry(retryProperties);
        CircuitBreakerRegistry circuitBreakerRegistry = configuration.circuitBreakerRegistry(circuitBreakerProperties);
        HttpResilienceExecutor executor = new HttpResilienceExecutor(retryRegistry, circuitBreakerRegistry);

        return new ResilientBotClient(new BotRestClient(restClient), executor);
    }

    private static RetryProperties retryProperties(int maxAttempts, Duration waitDuration) {
        RetryProperties properties = new RetryProperties();
        properties.setMaxAttempts(maxAttempts);
        properties.setWaitDuration(waitDuration);
        return properties;
    }

    private static CircuitBreakerProperties circuitBreakerProperties() {
        CircuitBreakerProperties properties = new CircuitBreakerProperties();
        properties.setSlidingWindowSize(10);
        properties.setMinimumNumberOfCalls(10);
        properties.setFailureRateThreshold(100);
        properties.setPermittedNumberOfCallsInHalfOpenState(2);
        properties.setWaitDurationInOpenState(Duration.ofSeconds(1));
        return properties;
    }

    private static LinkUpdateBatch batch() {
        return new LinkUpdateBatch(
                List.of(new LinkUpdate(1L, "https://github.com/owner/repo", "update", List.of(10L))));
    }
}
