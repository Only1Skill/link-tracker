package backend.academy.linktracker.scrapper.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.properties.LinkListCacheProperties;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
class ValkeyLinkListCacheIntegrationTest {
    private static final Long CHAT_ID = 123L;
    private static final String CACHE_KEY = String.valueOf(CHAT_ID);

    @Container
    private static final GenericContainer<?> VALKEY =
            new GenericContainer<>(DockerImageName.parse("valkey/valkey:8.0")).withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate stringRedisTemplate;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(VALKEY.getHost(), VALKEY.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        stringRedisTemplate = new StringRedisTemplate(connectionFactory);
        stringRedisTemplate.afterPropertiesSet();

        objectMapper = JsonMapper.builder().build();

        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void put_shouldStoreJsonByChatIdKey() {
        ValkeyLinkListCache cache = createCache(Duration.ofMinutes(10));
        List<LinkResponse> links = List.of(linkResponse(1L, "https://github.com/owner/repo"));

        cache.put(CHAT_ID, links);

        String json = stringRedisTemplate.opsForValue().get(CACHE_KEY);

        assertThat(json).isNotNull();
        assertThat(json).startsWith("[");
        assertThat(json).contains("https://github.com/owner/repo");
        assertThat(json).contains("java");
    }

    @Test
    void put_shouldSetConfiguredTtl() {
        ValkeyLinkListCache cache = createCache(Duration.ofMinutes(10));
        List<LinkResponse> links = List.of(linkResponse(1L, "https://github.com/owner/repo"));

        cache.put(CHAT_ID, links);

        Long ttlSeconds = stringRedisTemplate.getExpire(CACHE_KEY, TimeUnit.SECONDS);

        assertThat(ttlSeconds).isNotNull();
        assertThat(ttlSeconds).isPositive();
        assertThat(ttlSeconds).isLessThanOrEqualTo(Duration.ofMinutes(10).toSeconds());
    }

    @Test
    void get_shouldDeserializeStoredJson() {
        ValkeyLinkListCache cache = createCache(Duration.ofMinutes(10));
        List<LinkResponse> expected = List.of(linkResponse(1L, "https://github.com/owner/repo"));
        String json = objectMapper.writeValueAsString(expected);

        stringRedisTemplate.opsForValue().set(CACHE_KEY, json, Duration.ofMinutes(10));

        Optional<List<LinkResponse>> actual = cache.get(CHAT_ID);

        assertThat(actual).isPresent();
        assertThat(actual.orElseThrow()).isEqualTo(expected);
    }

    @Test
    void get_shouldReturnEmpty_whenKeyDoesNotExist() {
        ValkeyLinkListCache cache = createCache(Duration.ofMinutes(10));

        Optional<List<LinkResponse>> actual = cache.get(CHAT_ID);

        assertThat(actual).isEmpty();
    }

    @Test
    void get_shouldEvictInvalidJsonAndReturnEmpty() {
        ValkeyLinkListCache cache = createCache(Duration.ofMinutes(10));
        stringRedisTemplate.opsForValue().set(CACHE_KEY, "{invalid-json");

        Optional<List<LinkResponse>> actual = cache.get(CHAT_ID);

        assertThat(actual).isEmpty();
        assertThat(stringRedisTemplate.opsForValue().get(CACHE_KEY)).isNull();
    }

    @Test
    void evict_shouldDeleteChatKey() {
        ValkeyLinkListCache cache = createCache(Duration.ofMinutes(10));
        stringRedisTemplate.opsForValue().set(CACHE_KEY, "[]");

        cache.evict(CHAT_ID);

        assertThat(stringRedisTemplate.opsForValue().get(CACHE_KEY)).isNull();
    }

    @Test
    void put_shouldExpireEntryAfterTtl() {
        ValkeyLinkListCache cache = createCache(Duration.ofSeconds(1));
        List<LinkResponse> links = List.of(linkResponse(1L, "https://github.com/owner/repo"));

        cache.put(CHAT_ID, links);

        assertThat(stringRedisTemplate.opsForValue().get(CACHE_KEY)).isNotNull();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(
                        stringRedisTemplate.opsForValue().get(CACHE_KEY))
                .isNull());
    }

    private ValkeyLinkListCache createCache(Duration ttl) {
        LinkListCacheProperties properties = new LinkListCacheProperties();
        properties.setEnabled(true);
        properties.setTtl(ttl);

        return new ValkeyLinkListCache(stringRedisTemplate, objectMapper, properties);
    }

    private LinkResponse linkResponse(Long id, String url) {
        return new LinkResponse(
                id, URI.create(url), List.of("java", "backend"), OffsetDateTime.parse("2026-06-10T18:00:00Z"));
    }
}
