package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.cache.LinkListCache;
import backend.academy.linktracker.scrapper.cache.NoopLinkListCache;
import backend.academy.linktracker.scrapper.cache.ValkeyLinkListCache;
import backend.academy.linktracker.scrapper.properties.LinkListCacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class LinkCacheConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.cache.links.enabled", havingValue = "true", matchIfMissing = true)
    public LinkListCache valkeyLinkListCache(
            StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper, LinkListCacheProperties properties) {
        return new ValkeyLinkListCache(stringRedisTemplate, objectMapper, properties);
    }

    @Bean
    @ConditionalOnProperty(name = "app.cache.links.enabled", havingValue = "false")
    public LinkListCache noopLinkListCache() {
        return new NoopLinkListCache();
    }
}
