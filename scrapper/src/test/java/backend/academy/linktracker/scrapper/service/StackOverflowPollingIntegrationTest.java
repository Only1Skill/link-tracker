package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.BatchProcessingResult;
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
@TestPropertySource(
        properties = {
            "app.database.access-type=SQL",
            "app.scheduler.enabled=false",
            "spring.task.scheduling.enabled=false"
        })
class StackOverflowPollingIntegrationTest extends IntegrationTestBase {

    private static final long CHAT_ID = 202L;
    private static final String URL = "https://stackoverflow.com/questions/12345/how-to-write-tests";
    private static final OffsetDateTime BEFORE_EVENT_TIME = OffsetDateTime.parse("2026-04-16T09:00:00Z");

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

        wireMock.stubFor(
                post(urlEqualTo("/updates/batch")).willReturn(aResponse().withStatus(200)));
    }

    @Test
    void pollOnce_shouldSendStackOverflowAnswerNotificationBatch() {
        chatService.register(CHAT_ID);
        linkService.addLink(CHAT_ID, new AddLinkRequest(URL, List.of()));

        makeLinkEligibleForPolling(URL, BEFORE_EVENT_TIME);

        wireMock.stubFor(get(urlPathEqualTo("/questions/12345")).willReturn(okJson("""
                        {
                          "items": [
                            {
                              "question_id": 12345,
                              "title": "How to write tests?",
                              "creation_date": 1713250000,
                              "last_activity_date": 1713250100,
                              "link": "https://stackoverflow.com/questions/12345/how-to-write-tests",
                              "owner": { "display_name": "question-owner" }
                            }
                          ]
                        }
                        """)));

        wireMock.stubFor(get(urlPathEqualTo("/questions/12345/answers")).willReturn(okJson("""
                        {
                          "items": [
                            {
                              "answer_id": 10,
                              "question_id": 12345,
                              "body": "This is a brand new answer",
                              "creation_date": 1892282400,
                              "last_activity_date": 1892282400,
                              "link": "https://stackoverflow.com/a/10",
                              "owner": { "display_name": "answer-author" }
                            }
                          ]
                        }
                        """)));

        wireMock.stubFor(get(urlPathEqualTo("/questions/12345/comments")).willReturn(okJson("""
                        {
                          "items": []
                        }
                        """)));

        BatchProcessingResult result = linkPollingService.pollOnce();

        assertEquals(1, result.total());
        assertEquals(1, result.successCount());
        assertEquals(0, result.failedCount());
        assertEquals(1, result.updatedLinksCount());

        wireMock.verify(
                1,
                postRequestedFor(urlEqualTo("/updates/batch"))
                        .withRequestBody(containing("updates"))
                        .withRequestBody(containing("How to write tests?"))
                        .withRequestBody(containing("answer-author"))
                        .withRequestBody(containing(URL))
                        .withRequestBody(containing(String.valueOf(CHAT_ID))));

        wireMock.verify(0, postRequestedFor(urlEqualTo("/updates")));
    }

    private void makeLinkEligibleForPolling(String url, OffsetDateTime lastProcessedAt) {
        jdbcTemplate.update(
                "UPDATE links SET last_check_time = ?, last_update_time = ? WHERE url = ?",
                Timestamp.from(lastProcessedAt.toInstant()),
                Timestamp.from(lastProcessedAt.toInstant()),
                url);
    }
}
