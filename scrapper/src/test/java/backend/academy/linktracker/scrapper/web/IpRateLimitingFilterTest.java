package backend.academy.linktracker.scrapper.web;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.configuration.ResilienceConfiguration;
import backend.academy.linktracker.scrapper.properties.RateLimitingProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IpRateLimitingFilterTest {

    @Test
    void doFilterInternal_shouldReturn429AfterIpLimitExceeded() throws Exception {
        IpRateLimitingFilter filter =
                new IpRateLimitingFilter(properties(), new ResilienceConfiguration().rateLimiterConfig(properties()));

        MockHttpServletRequest firstRequest = request("10.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(HttpStatus.OK.value());

        MockHttpServletRequest secondRequest = request("10.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertThat(secondResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilterInternal_shouldUseSeparateLimitForDifferentIps() throws Exception {
        IpRateLimitingFilter filter =
                new IpRateLimitingFilter(properties(), new ResilienceConfiguration().rateLimiterConfig(properties()));

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        filter.doFilter(request("10.0.0.1"), firstResponse, new MockFilterChain());
        filter.doFilter(request("10.0.0.2"), secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(secondResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    private static RateLimitingProperties properties() {
        RateLimitingProperties properties = new RateLimitingProperties();
        properties.setLimitForPeriod(1);
        properties.setLimitRefreshPeriod(Duration.ofMinutes(1));
        return properties;
    }

    private static MockHttpServletRequest request(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/links");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }
}
