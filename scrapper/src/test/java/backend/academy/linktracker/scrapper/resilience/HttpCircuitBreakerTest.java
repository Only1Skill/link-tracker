package backend.academy.linktracker.scrapper.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.configuration.ResilienceConfiguration;
import backend.academy.linktracker.scrapper.properties.CircuitBreakerProperties;
import backend.academy.linktracker.scrapper.properties.RetryProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;

class HttpCircuitBreakerTest {

    private static final String CLIENT_NAME = "test-client";

    private final ResilienceConfiguration configuration = new ResilienceConfiguration();

    @Test
    void execute_shouldOpenCircuitBreakerAfterFailureThresholdAndRejectCallsImmediately() {
        CircuitBreakerRegistry circuitBreakerRegistry =
                configuration.circuitBreakerRegistry(circuitBreakerProperties());
        HttpResilienceExecutor executor = executor(circuitBreakerRegistry);
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> executor.execute(CLIENT_NAME, () -> failingCall(calls)))
                .isInstanceOf(RestClientResponseException.class);
        assertThatThrownBy(() -> executor.execute(CLIENT_NAME, () -> failingCall(calls)))
                .isInstanceOf(RestClientResponseException.class);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CLIENT_NAME);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> executor.execute(CLIENT_NAME, () -> failingCall(calls)))
                .isInstanceOf(CallNotPermittedException.class);
        assertThat(calls).hasValue(2);
    }

    @Test
    void execute_shouldCloseCircuitBreakerAfterSuccessfulHalfOpenCalls() throws InterruptedException {
        CircuitBreakerRegistry circuitBreakerRegistry =
                configuration.circuitBreakerRegistry(circuitBreakerProperties());
        HttpResilienceExecutor executor = executor(circuitBreakerRegistry);

        assertThatThrownBy(() -> executor.execute(CLIENT_NAME, () -> failingCall(new AtomicInteger())))
                .isInstanceOf(RestClientResponseException.class);
        assertThatThrownBy(() -> executor.execute(CLIENT_NAME, () -> failingCall(new AtomicInteger())))
                .isInstanceOf(RestClientResponseException.class);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CLIENT_NAME);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        Thread.sleep(150);

        assertThat(executor.execute(CLIENT_NAME, () -> "ok")).isEqualTo("ok");
        assertThat(executor.execute(CLIENT_NAME, () -> "ok")).isEqualTo("ok");

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void execute_shouldReopenCircuitBreakerAfterFailedHalfOpenCall() throws InterruptedException {
        CircuitBreakerProperties properties = circuitBreakerProperties();
        properties.setPermittedNumberOfCallsInHalfOpenState(1);
        CircuitBreakerRegistry circuitBreakerRegistry = configuration.circuitBreakerRegistry(properties);
        HttpResilienceExecutor executor = executor(circuitBreakerRegistry);

        assertThatThrownBy(() -> executor.execute(CLIENT_NAME, () -> failingCall(new AtomicInteger())))
                .isInstanceOf(RestClientResponseException.class);
        assertThatThrownBy(() -> executor.execute(CLIENT_NAME, () -> failingCall(new AtomicInteger())))
                .isInstanceOf(RestClientResponseException.class);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CLIENT_NAME);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        Thread.sleep(150);

        assertThatThrownBy(() -> executor.execute(CLIENT_NAME, () -> failingCall(new AtomicInteger())))
                .isInstanceOf(RestClientResponseException.class);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    private HttpResilienceExecutor executor(CircuitBreakerRegistry circuitBreakerRegistry) {
        RetryProperties retryProperties = new RetryProperties();
        retryProperties.setMaxAttempts(1);
        RetryRegistry retryRegistry = configuration.retryRegistry(retryProperties);
        return new HttpResilienceExecutor(retryRegistry, circuitBreakerRegistry);
    }

    private static CircuitBreakerProperties circuitBreakerProperties() {
        CircuitBreakerProperties properties = new CircuitBreakerProperties();
        properties.setSlidingWindowSize(2);
        properties.setMinimumNumberOfCalls(2);
        properties.setFailureRateThreshold(100);
        properties.setPermittedNumberOfCallsInHalfOpenState(2);
        properties.setWaitDurationInOpenState(Duration.ofMillis(100));
        return properties;
    }

    private static String failingCall(AtomicInteger calls) {
        calls.incrementAndGet();
        throw HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "server error",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8);
    }
}
