package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.test.IntegrationTestBase;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(properties = "app.database.access-type=ORM")
class GitHubPollingIntegrationTest extends IntegrationTestBase {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void registerWireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("app.github.base-url", wireMock::baseUrl);
        registry.add("app.bot.base-url", wireMock::baseUrl);
        registry.add("app.stackoverflow.base-url", wireMock::baseUrl);

        registry.add("app.github.token", () -> "test-token");
        registry.add("app.stackoverflow.key", () -> "test-key");
        registry.add("app.stackoverflow.access-token", () -> "test-access-token");
    }

    @Autowired
    private ChatService chatService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private LinkPollingService linkPollingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();

        wireMock.stubFor(post(urlEqualTo("/updates")).willReturn(aResponse().withStatus(200)));
    }

    @Test
    void pollOnce_shouldSendGithubIssueNotification() {
        long chatId = 101L;
        String url = "https://github.com/test-owner/test-repo";

        chatService.register(chatId);
        linkService.addLink(chatId, new AddLinkRequest(url, List.of()));

        OffsetDateTime oldTime = OffsetDateTime.now().minusDays(1);
        jdbcTemplate.update(
                "UPDATE links SET last_check_time = ?, last_update_time = ? WHERE url = ?",
                Timestamp.from(oldTime.toInstant()),
                Timestamp.from(oldTime.toInstant()),
                url);

        wireMock.stubFor(
                get(urlPathEqualTo("/repos/test-owner/test-repo/issues")).willReturn(okJson("""
                        [
                          {
                            "id": 1,
                            "title": "New issue title",
                            "body": "Issue body preview",
                            "created_at": "2026-04-16T10:10:00Z",
                            "updated_at": "2026-04-16T10:10:00Z",
                            "html_url": "https://github.com/test-owner/test-repo/issues/1",
                            "user": { "login": "alice" }
                          }
                        ]
                        """)));

        linkPollingService.pollOnce();

        wireMock.verify(
                1,
                postRequestedFor(urlEqualTo("/updates"))
                        .withRequestBody(containing("New issue title"))
                        .withRequestBody(containing("alice"))
                        .withRequestBody(containing(url)));
    }
}
