package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.SubscriptionLinkView;
import backend.academy.linktracker.scrapper.repository.ChatStorage;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import backend.academy.linktracker.scrapper.test.IntegrationTestBase;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractLinkCrudIntegrationTest extends IntegrationTestBase {

    private static final long FIRST_CHAT_ID = 1001L;
    private static final long SECOND_CHAT_ID = 1002L;
    private static final OffsetDateTime LAST_CHECK_TIME = OffsetDateTime.parse("2026-04-16T10:00:00Z");
    private static final OffsetDateTime LAST_UPDATE_TIME = OffsetDateTime.parse("2026-04-16T09:00:00Z");

    @Autowired
    protected ChatStorage chatStorage;

    @Autowired
    protected LinkStorage linkStorage;

    @BeforeEach
    void registerChats() {
        chatStorage.save(FIRST_CHAT_ID);
        chatStorage.save(SECOND_CHAT_ID);
    }

    @Test
    void save_shouldPersistSubscriptionWithTags() {
        SubscriptionLinkView saved = linkStorage.save(
                FIRST_CHAT_ID,
                newLink("https://github.com/user/repo", List.of("work", "backend"))
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getChatId()).isEqualTo(FIRST_CHAT_ID);
        assertThat(saved.getUrl()).isEqualTo("https://github.com/user/repo");
        assertThat(saved.getTags()).containsExactlyInAnyOrder("work", "backend");

        List<SubscriptionLinkView> links = linkStorage.findByChatId(FIRST_CHAT_ID);

        assertThat(links).hasSize(1);
        assertThat(links.getFirst().getUrl()).isEqualTo("https://github.com/user/repo");
        assertThat(links.getFirst().getTags()).containsExactlyInAnyOrder("work", "backend");
    }

    @Test
    void sameUrlForDifferentChats_shouldKeepOwnTagsPerSubscription() {
        String url = "https://github.com/shared/repo";

        linkStorage.save(FIRST_CHAT_ID, newLink(url, List.of("work")));
        linkStorage.save(SECOND_CHAT_ID, newLink(url, List.of("study")));

        List<SubscriptionLinkView> firstChatLinks = linkStorage.findByChatId(FIRST_CHAT_ID);
        List<SubscriptionLinkView> secondChatLinks = linkStorage.findByChatId(SECOND_CHAT_ID);

        assertThat(firstChatLinks).hasSize(1);
        assertThat(secondChatLinks).hasSize(1);

        assertThat(firstChatLinks.getFirst().getTags()).containsExactly("work");
        assertThat(secondChatLinks.getFirst().getTags()).containsExactly("study");
    }

    @Test
    void findByChatIdWithTag_shouldReturnOnlyMatchingSubscriptions() {
        linkStorage.save(FIRST_CHAT_ID, newLink("https://github.com/user/repo-1", List.of("work")));
        linkStorage.save(FIRST_CHAT_ID, newLink("https://github.com/user/repo-2", List.of("study")));
        linkStorage.save(SECOND_CHAT_ID, newLink("https://github.com/user/repo-3", List.of("work")));

        List<SubscriptionLinkView> result = linkStorage.findByChatId(FIRST_CHAT_ID, "work");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUrl()).isEqualTo("https://github.com/user/repo-1");
        assertThat(result.getFirst().getTags()).containsExactly("work");
    }

    @Test
    void findByChatIdAndUrl_shouldReturnSubscriptionWithChatSpecificTags() {
        String url = "https://stackoverflow.com/questions/123456/test";
        linkStorage.save(FIRST_CHAT_ID, newLink(url, List.of("java")));
        linkStorage.save(SECOND_CHAT_ID, newLink(url, List.of("sql")));

        SubscriptionLinkView firstChatLink =
                linkStorage.findByChatIdAndUrl(FIRST_CHAT_ID, url).orElseThrow();

        SubscriptionLinkView secondChatLink =
                linkStorage.findByChatIdAndUrl(SECOND_CHAT_ID, url).orElseThrow();

        assertThat(firstChatLink.getTags()).containsExactly("java");
        assertThat(secondChatLink.getTags()).containsExactly("sql");
    }

    @Test
    void delete_shouldRemoveOnlySubscriptionOfSelectedChat() {
        String url = "https://github.com/shared/repo";

        linkStorage.save(FIRST_CHAT_ID, newLink(url, List.of("work")));
        linkStorage.save(SECOND_CHAT_ID, newLink(url, List.of("study")));

        linkStorage.delete(FIRST_CHAT_ID, url);

        assertThat(linkStorage.findByChatId(FIRST_CHAT_ID)).isEmpty();
        assertThat(linkStorage.findByChatId(SECOND_CHAT_ID)).hasSize(1);
        assertThat(linkStorage.findByChatId(SECOND_CHAT_ID).getFirst().getTags()).containsExactly("study");

        List<SubscriptionLinkView> allByUrl = linkStorage.findAllByUrl(url);
        assertThat(allByUrl).hasSize(1);
        assertThat(allByUrl.getFirst().getChatId()).isEqualTo(SECOND_CHAT_ID);
    }

    @Test
    void deleteByChatId_shouldRemoveOnlyChatSubscriptionsAndKeepSharedLinksForOthers() {
        String sharedUrl = "https://github.com/shared/repo";
        String ownUrl = "https://github.com/own/repo";

        linkStorage.save(FIRST_CHAT_ID, newLink(sharedUrl, List.of("work")));
        linkStorage.save(FIRST_CHAT_ID, newLink(ownUrl, List.of("backend")));
        linkStorage.save(SECOND_CHAT_ID, newLink(sharedUrl, List.of("study")));

        linkStorage.deleteByChatId(FIRST_CHAT_ID);

        assertThat(linkStorage.findByChatId(FIRST_CHAT_ID)).isEmpty();

        List<SubscriptionLinkView> secondChatLinks = linkStorage.findByChatId(SECOND_CHAT_ID);
        assertThat(secondChatLinks).hasSize(1);
        assertThat(secondChatLinks.getFirst().getUrl()).isEqualTo(sharedUrl);
        assertThat(secondChatLinks.getFirst().getTags()).containsExactly("study");

        assertThat(linkStorage.findAllByUrl(ownUrl)).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllSubscriptionsWithOwnTags() {
        linkStorage.save(FIRST_CHAT_ID, newLink("https://github.com/user/repo-1", List.of("tag1")));
        linkStorage.save(SECOND_CHAT_ID, newLink("https://github.com/user/repo-2", List.of("tag2")));

        List<SubscriptionLinkView> all = linkStorage.findAll();

        assertThat(all).hasSize(2);
        assertThat(all)
                .extracting(SubscriptionLinkView::getChatId)
                .containsExactlyInAnyOrder(FIRST_CHAT_ID, SECOND_CHAT_ID);
        assertThat(all)
                .extracting(SubscriptionLinkView::getUrl)
                .containsExactlyInAnyOrder(
                        "https://github.com/user/repo-1",
                        "https://github.com/user/repo-2"
                );
    }

    private SubscriptionLinkView newLink(String url, List<String> tags) {
        return SubscriptionLinkView.builder()
                .url(url)
                .tags(tags)
                .lastCheckTime(LAST_CHECK_TIME)
                .lastUpdateTime(LAST_UPDATE_TIME)
                .build();
    }
}
