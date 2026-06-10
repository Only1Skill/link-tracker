package backend.academy.linktracker.scrapper.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.service.ChatService;
import backend.academy.linktracker.scrapper.test.IntegrationTestBase;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(LinkListCacheApiIntegrationTest.ValkeyTestConfiguration.class)
@TestPropertySource(
        properties = {"app.database.access-type=SQL", "app.cache.links.enabled=true", "app.cache.links.ttl=10m"})
class LinkListCacheApiIntegrationTest extends IntegrationTestBase {
    private static final Long CHAT_ID = 123L;
    private static final String CACHE_KEY = String.valueOf(CHAT_ID);

    private static final GenericContainer<?> VALKEY =
            new GenericContainer<>(DockerImageName.parse("valkey/valkey:8.0")).withExposedPorts(6379);

    static {
        VALKEY.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ChatService chatService;

    @BeforeEach
    void cleanCache() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    void getLinks_shouldStoreResponseInValkeyByChatIdKey() throws Exception {
        chatService.register(CHAT_ID);
        addLink(CHAT_ID, "https://github.com/test-owner/test-repo", List.of("java", "backend"));

        mockMvc.perform(get("/links").header("Tg-Chat-Id", CHAT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].url").value("https://github.com/test-owner/test-repo"))
                .andExpect(jsonPath("$[0].tags").isArray())
                .andExpect(jsonPath("$[0].tags.length()").value(2))
                .andExpect(jsonPath("$[0].tags[?(@ == 'java')]").exists())
                .andExpect(jsonPath("$[0].tags[?(@ == 'backend')]").exists());

        String cachedJson = stringRedisTemplate.opsForValue().get(CACHE_KEY);

        assertThat(cachedJson).isNotNull();
        assertThat(cachedJson).startsWith("[");
        assertThat(cachedJson).contains("https://github.com/test-owner/test-repo");
        assertThat(cachedJson).contains("java");
        assertThat(cachedJson).contains("backend");
    }

    @Test
    void getLinks_shouldReturnCachedResponse_whenCacheHit() throws Exception {
        chatService.register(CHAT_ID);

        LinkResponse cachedLink = new LinkResponse(
                99L,
                URI.create("https://github.com/cached-owner/cached-repo"),
                List.of("cached"),
                OffsetDateTime.parse("2026-06-10T18:00:00Z"));

        String cachedJson = objectMapper.writeValueAsString(List.of(cachedLink));
        stringRedisTemplate.opsForValue().set(CACHE_KEY, cachedJson);

        mockMvc.perform(get("/links").header("Tg-Chat-Id", CHAT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(99L))
                .andExpect(jsonPath("$[0].url").value("https://github.com/cached-owner/cached-repo"))
                .andExpect(jsonPath("$[0].tags[0]").value("cached"));
    }

    @Test
    void addLink_shouldInvalidateCachedList() throws Exception {
        chatService.register(CHAT_ID);
        stringRedisTemplate.opsForValue().set(CACHE_KEY, "[]");

        AddLinkRequest request = new AddLinkRequest("https://github.com/test-owner/test-repo", List.of("java"));

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(request.link()));

        assertThat(stringRedisTemplate.opsForValue().get(CACHE_KEY)).isNull();
    }

    @Test
    void deleteLink_shouldInvalidateCachedList() throws Exception {
        chatService.register(CHAT_ID);
        String url = "https://github.com/test-owner/test-repo";
        addLink(CHAT_ID, url, List.of("java"));

        stringRedisTemplate.opsForValue().set(CACHE_KEY, "[]");

        AddLinkRequest request = new AddLinkRequest(url, List.of());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(stringRedisTemplate.opsForValue().get(CACHE_KEY)).isNull();
    }

    @Test
    void getLinksWithTag_shouldBypassChatIdCache() throws Exception {
        chatService.register(CHAT_ID);

        addLink(CHAT_ID, "https://github.com/test-owner/java-repo", List.of("java"));
        addLink(CHAT_ID, "https://github.com/test-owner/kotlin-repo", List.of("kotlin"));

        LinkResponse cachedFullListMarker = new LinkResponse(
                999L,
                URI.create("https://github.com/cached-owner/full-list-marker"),
                List.of("cached"),
                OffsetDateTime.parse("2026-06-10T18:00:00Z"));

        stringRedisTemplate
                .opsForValue()
                .set(CACHE_KEY, objectMapper.writeValueAsString(List.of(cachedFullListMarker)));

        mockMvc.perform(get("/links").header("Tg-Chat-Id", CHAT_ID).param("tag", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].url").value("https://github.com/test-owner/java-repo"))
                .andExpect(jsonPath("$[0].tags[0]").value("java"));
    }

    @Test
    void deleteChat_shouldInvalidateCachedList() throws Exception {
        chatService.register(CHAT_ID);
        stringRedisTemplate.opsForValue().set(CACHE_KEY, "[]");

        mockMvc.perform(delete("/tg-chat/{id}", CHAT_ID)).andExpect(status().isOk());

        assertThat(stringRedisTemplate.opsForValue().get(CACHE_KEY)).isNull();
    }

    private void addLink(Long chatId, String url, List<String> tags) throws Exception {
        AddLinkRequest request = new AddLinkRequest(url, tags);

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class ValkeyTestConfiguration {

        @Bean
        @Primary
        LettuceConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory(VALKEY.getHost(), VALKEY.getMappedPort(6379));
        }

        @Bean
        @Primary
        StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
            StringRedisTemplate template = new StringRedisTemplate(redisConnectionFactory);
            template.afterPropertiesSet();
            return template;
        }
    }
}
