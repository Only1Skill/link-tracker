package backend.academy.linktracker.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HttpResilienceExecutor {

    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public <T> T execute(String clientName, Supplier<T> request) {
        Retry retry = retryRegistry.retry(clientName);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(clientName);

        Supplier<T> retriedRequest = Retry.decorateSupplier(retry, request);
        Supplier<T> protectedRequest = CircuitBreaker.decorateSupplier(circuitBreaker, retriedRequest);

        return protectedRequest.get();
    }

    public void executeVoid(String clientName, Runnable request) {
        execute(clientName, () -> {
            request.run();
            return null;
        });
    }
}
