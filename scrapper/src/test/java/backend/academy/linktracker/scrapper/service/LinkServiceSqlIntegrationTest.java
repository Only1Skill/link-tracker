//package backend.academy.linktracker.scrapper.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//
//import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
//import backend.academy.linktracker.scrapper.dto.LinkResponse;
//import backend.academy.linktracker.scrapper.exception.LinkDuplicateException;
//import backend.academy.linktracker.scrapper.test.IntegrationTestBase;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicLong;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.TestPropertySource;
//
//@ActiveProfiles("test")
//@TestPropertySource(properties = "app.database.access-type=SQL")
//class LinkServiceSqlIntegrationTest extends IntegrationTestBase {
//
//    @Autowired
//    private LinkService linkService;
//
//    @Autowired
//    private ChatService chatService;
//
//    private static final AtomicLong CHAT_ID_COUNTER = new AtomicLong();
//    private long chatId;
//
//    @BeforeEach
//    void setUp() {
//        chatId = CHAT_ID_COUNTER.incrementAndGet();
//        chatService.register(chatId);
//    }
//
//    @Test
//    void shouldAddLink() {
//        AddLinkRequest request = new AddLinkRequest("https://github.com/test/repo", List.of("test"));
//        LinkResponse response = linkService.addLink(chatId, request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.url().toString()).isEqualTo(request.link());
//        assertThat(response.tags()).containsExactly("test");
//    }
//
//    @Test
//    void shouldGetLinks() {
//        linkService.addLink(chatId, new AddLinkRequest("https://github.com/test/repo1", List.of("java")));
//        linkService.addLink(chatId, new AddLinkRequest("https://github.com/test/repo2", List.of("python")));
//
//        List<LinkResponse> links = linkService.getLinks(chatId, null);
//        assertThat(links).hasSize(2);
//    }
//
//    @Test
//    void shouldThrowWhenDuplicateLink() {
//        AddLinkRequest request = new AddLinkRequest("https://github.com/test/repo", List.of());
//
//        linkService.addLink(chatId, request);
//        assertThatThrownBy(() -> linkService.addLink(chatId, request)).isInstanceOf(LinkDuplicateException.class);
//    }
//}
