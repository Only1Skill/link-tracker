package backend.academy.linktracker.scrapper.repository.inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.model.SubscriptionLinkView;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryLinkStorageTest {
    private LinkStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryLinkStorage();
    }

    @Test
    void save_assignsIdAndStores() {
        SubscriptionLinkView link = SubscriptionLinkView.builder()
                .chatId(1L)
                .url("http://example.com")
                .build();
        SubscriptionLinkView saved = storage.save(1L, link);
        assertThat(saved.getId()).isNotNull();
        assertThat(storage.findByChatId(1L)).containsExactly(saved);
    }

    @Test
    void findByChatId_returnsEmptyList_whenNoLinks() {
        assertThat(storage.findByChatId(1L)).isEmpty();
    }

    @Test
    void findByChatIdAndUrl_returnsLink_whenExists() {
        storage.save(
                1L,
                SubscriptionLinkView.builder()
                        .chatId(1L)
                        .url("http://example.com")
                        .build());
        assertThat(storage.findByChatIdAndUrl(1L, "http://example.com")).isPresent();
    }

    @Test
    void delete_removesLink() {
        storage.save(
                1L,
                SubscriptionLinkView.builder()
                        .chatId(1L)
                        .url("http://example.com")
                        .build());
        storage.delete(1L, "http://example.com");
        assertThat(storage.findByChatIdAndUrl(1L, "http://example.com")).isEmpty();
    }

    @Test
    void deleteByChatId_removesAllLinksForChat() {
        storage.save(1L, SubscriptionLinkView.builder().chatId(1L).url("a").build());
        storage.save(1L, SubscriptionLinkView.builder().chatId(1L).url("b").build());
        storage.deleteByChatId(1L);
        assertThat(storage.findByChatId(1L)).isEmpty();
    }

    @Test
    void findAll_returnsAllLinks() {
        storage.save(1L, SubscriptionLinkView.builder().chatId(1L).url("a").build());
        storage.save(2L, SubscriptionLinkView.builder().chatId(2L).url("b").build());
        assertThat(storage.findAll()).hasSize(2);
    }

    @Test
    void findAllByUrl_returnsLinksWithThatUrl() {
        storage.save(
                1L,
                SubscriptionLinkView.builder()
                        .chatId(1L)
                        .url("http://example.com")
                        .build());
        storage.save(
                2L,
                SubscriptionLinkView.builder()
                        .chatId(2L)
                        .url("http://example.com")
                        .build());
        assertThat(storage.findAllByUrl("http://example.com")).hasSize(2);
    }
}
