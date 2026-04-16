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
@TestPropertySource(properties = "app.database.access-type=SQL")
class StackOverflowPollingIntegrationTest extends IntegrationTestBase {

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
    void pollOnce_shouldSendStackOverflowAnswerNotification() {
        long chatId = 202L;
        String url = "https://stackoverflow.com/questions/12345/how-to-write-tests";

        chatService.register(chatId);
        linkService.addLink(chatId, new AddLinkRequest(url, List.of()));

        OffsetDateTime oldTime = OffsetDateTime.now().minusDays(1);
        jdbcTemplate.update(
                "UPDATE links SET last_check_time = ?, last_update_time = ? WHERE url = ?",
                Timestamp.from(oldTime.toInstant()),
                Timestamp.from(oldTime.toInstant()),
                url);

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

        linkPollingService.pollOnce();

        wireMock.verify(
                1,
                postRequestedFor(urlEqualTo("/updates"))
                        .withRequestBody(containing("How to write tests?"))
                        .withRequestBody(containing("answer-author"))
                        .withRequestBody(containing(url)));
    }
}
