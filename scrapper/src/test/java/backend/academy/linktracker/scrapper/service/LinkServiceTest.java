package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkDuplicateException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.repository.ChatStorage;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import backend.academy.linktracker.scrapper.repository.inmemory.InMemoryChatStorage;
import backend.academy.linktracker.scrapper.repository.inmemory.InMemoryLinkStorage;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkServiceTest {
    private LinkStorage linkStorage;
    private ChatStorage chatStorage;
    private LinkService linkService;

    @BeforeEach
    void setUp() {
        linkStorage = new InMemoryLinkStorage();
        chatStorage = new InMemoryChatStorage();
        linkService = new LinkService(linkStorage, chatStorage);
    }

    @Test
    void addLink_shouldReturnResponse_whenChatExistsAndLinkNotDuplicate() {
        chatStorage.save(1L);
        AddLinkRequest request = new AddLinkRequest("https://github.com/user/repo", List.of("tag1"));
        LinkResponse response = linkService.addLink(1L, request);

        assertThat(response.id()).isNotNull();
        assertThat(response.url()).isEqualTo(URI.create("https://github.com/user/repo"));
        assertThat(response.tags()).containsExactly("tag1");
    }

    @Test
    void addLink_shouldThrow_whenChatNotExists() {
        AddLinkRequest request = new AddLinkRequest("url", null);
        assertThatThrownBy(() -> linkService.addLink(1L, request)).isInstanceOf(ChatNotFoundException.class);
    }

    @Test
    void addLink_shouldThrow_whenLinkAlreadyExists() {
        chatStorage.save(1L);
        AddLinkRequest request = new AddLinkRequest("https://github.com/user/repo", null);
        linkService.addLink(1L, request);
        assertThatThrownBy(() -> linkService.addLink(1L, request)).isInstanceOf(LinkDuplicateException.class);
    }

    @Test
    void getLinks_shouldReturnAllLinks_whenNoTag() {
        chatStorage.save(1L);
        linkService.addLink(1L, new AddLinkRequest("url1", null));
        linkService.addLink(1L, new AddLinkRequest("url2", List.of("tag2")));

        List<LinkResponse> links = linkService.getLinks(1L, null);
        assertThat(links).hasSize(2);
    }

    @Test
    void getLinks_shouldFilterByTag() {
        chatStorage.save(1L);
        linkService.addLink(1L, new AddLinkRequest("url1", List.of("tag1")));
        linkService.addLink(1L, new AddLinkRequest("url2", List.of("tag2")));

        List<LinkResponse> links = linkService.getLinks(1L, "tag1");
        assertThat(links).hasSize(1);
        assertThat(links.getFirst().url().toString()).isEqualTo("url1");
    }

    @Test
    void getLinks_shouldThrow_whenChatNotExists() {
        assertThatThrownBy(() -> linkService.getLinks(1L, null)).isInstanceOf(ChatNotFoundException.class);
    }

    @Test
    void removeLink_shouldDelete_whenChatExistsAndLinkExists() {
        chatStorage.save(1L);
        linkService.addLink(1L, new AddLinkRequest("url", null));
        linkService.removeLink(1L, "url");
        assertThat(linkService.getLinks(1L, null)).isEmpty();
    }

    @Test
    void removeLink_shouldThrow_whenChatNotExists() {
        assertThatThrownBy(() -> linkService.removeLink(1L, "url")).isInstanceOf(ChatNotFoundException.class);
    }

    @Test
    void removeLink_shouldThrow_whenLinkNotFound() {
        chatStorage.save(1L);
        assertThatThrownBy(() -> linkService.removeLink(1L, "nonexistent")).isInstanceOf(LinkNotFoundException.class);
    }
}
