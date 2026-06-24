package backend.academy.linktracker.configuration;

import backend.academy.linktracker.exception.ScrapperClientException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Configuration
public class ResilienceConfiguration {

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory(HttpClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return requestFactory;
    }

    @Bean
    public RetryRegistry retryRegistry(RetryProperties properties) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.getMaxAttempts())
                .waitDuration(properties.getWaitDuration())
                .retryOnException(throwable -> isRetryableHttpStatus(throwable, properties))
                .build();
        return RetryRegistry.of(config);
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerProperties properties) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(properties.getSlidingWindowSize())
                .minimumNumberOfCalls(properties.getMinimumNumberOfCalls())
                .failureRateThreshold(properties.getFailureRateThreshold())
                .permittedNumberOfCallsInHalfOpenState(properties.getPermittedNumberOfCallsInHalfOpenState())
                .waitDurationInOpenState(properties.getWaitDurationInOpenState())
                .recordException(ResilienceConfiguration::isCircuitBreakerFailure)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RateLimiterConfig rateLimiterConfig(RateLimitingProperties properties) {
        return RateLimiterConfig.custom()
                .limitForPeriod(properties.getLimitForPeriod())
                .limitRefreshPeriod(properties.getLimitRefreshPeriod())
                .timeoutDuration(Duration.ZERO)
                .build();
    }

    private static boolean isRetryableHttpStatus(Throwable throwable, RetryProperties properties) {
        if (throwable instanceof RestClientResponseException exception) {
            HttpStatusCode statusCode = exception.getStatusCode();
            return properties.getRetryableStatuses().contains(statusCode.value());
        }

        if (throwable instanceof ScrapperClientException exception && exception.getStatusCode() != null) {
            return properties.getRetryableStatuses().contains(exception.getStatusCode());
        }

        return false;
    }

    private static boolean isCircuitBreakerFailure(Throwable throwable) {
        if (throwable instanceof RestClientResponseException exception) {
            return exception.getStatusCode().is5xxServerError();
        }

        if (throwable instanceof ScrapperClientException exception && exception.getStatusCode() != null) {
            return exception.getStatusCode() >= 500;
        }

        if (throwable instanceof ResourceAccessException exception) {
            return hasTimeoutCause(exception);
        }

        return true;
    }

    private static boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
