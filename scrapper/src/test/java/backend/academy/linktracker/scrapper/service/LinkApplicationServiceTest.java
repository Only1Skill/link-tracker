package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.cache.LinkListCache;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkApplicationServiceTest {
    private static final Long CHAT_ID = 123L;

    @Mock
    private LinkService linkService;

    @Mock
    private LinkListCache linkListCache;

    @InjectMocks
    private LinkApplicationService linkApplicationService;

    @Test
    void getLinks_shouldReturnCachedLinks_whenCacheHit() {
        List<LinkResponse> cachedLinks = List.of(linkResponse("https://github.com/owner/repo"));

        when(linkListCache.get(CHAT_ID)).thenReturn(Optional.of(cachedLinks));

        List<LinkResponse> actual = linkApplicationService.getLinks(CHAT_ID, null);

        assertThat(actual).isEqualTo(cachedLinks);
        verify(linkListCache).get(CHAT_ID);
        verifyNoInteractions(linkService);
        verify(linkListCache, never()).put(anyLong(), anyList());
    }

    @Test
    void getLinks_shouldLoadAndCacheLinks_whenCacheMiss() {
        List<LinkResponse> loadedLinks = List.of(linkResponse("https://github.com/owner/repo"));

        when(linkListCache.get(CHAT_ID)).thenReturn(Optional.empty());
        when(linkService.getLinks(CHAT_ID, null)).thenReturn(loadedLinks);

        List<LinkResponse> actual = linkApplicationService.getLinks(CHAT_ID, null);

        assertThat(actual).isEqualTo(loadedLinks);
        verify(linkListCache).get(CHAT_ID);
        verify(linkService).getLinks(CHAT_ID, null);
        verify(linkListCache).put(CHAT_ID, loadedLinks);
    }

    @Test
    void getLinks_shouldBypassCache_whenTagProvided() {
        String tag = "java";
        List<LinkResponse> taggedLinks = List.of(linkResponse("https://github.com/owner/repo"));

        when(linkService.getLinks(CHAT_ID, tag)).thenReturn(taggedLinks);

        List<LinkResponse> actual = linkApplicationService.getLinks(CHAT_ID, tag);

        assertThat(actual).isEqualTo(taggedLinks);
        verify(linkService).getLinks(CHAT_ID, tag);
        verifyNoInteractions(linkListCache);
    }

    @Test
    void getLinks_shouldBypassCache_whenBlankTagProvided() {
        List<LinkResponse> loadedLinks = List.of(linkResponse("https://github.com/owner/repo"));

        when(linkListCache.get(CHAT_ID)).thenReturn(Optional.empty());
        when(linkService.getLinks(CHAT_ID, null)).thenReturn(loadedLinks);

        List<LinkResponse> actual = linkApplicationService.getLinks(CHAT_ID, " ");

        assertThat(actual).isEqualTo(loadedLinks);
        verify(linkListCache).get(CHAT_ID);
        verify(linkService).getLinks(CHAT_ID, null);
        verify(linkListCache).put(CHAT_ID, loadedLinks);
    }

    @Test
    void addLink_shouldEvictCacheAfterSuccessfulAdd() {
        AddLinkRequest request = new AddLinkRequest("https://github.com/owner/repo", List.of("java"));
        LinkResponse response = linkResponse(request.link());

        when(linkService.addLink(CHAT_ID, request)).thenReturn(response);

        LinkResponse actual = linkApplicationService.addLink(CHAT_ID, request);

        assertThat(actual).isEqualTo(response);
        verify(linkService).addLink(CHAT_ID, request);
        verify(linkListCache).evict(CHAT_ID);
    }

    @Test
    void addLink_shouldNotEvictCache_whenServiceThrows() {
        AddLinkRequest request = new AddLinkRequest("https://github.com/owner/repo", List.of("java"));
        RuntimeException exception = new RuntimeException("add failed");

        when(linkService.addLink(CHAT_ID, request)).thenThrow(exception);

        assertThatThrownBy(() -> linkApplicationService.addLink(CHAT_ID, request))
                .isSameAs(exception);

        verify(linkService).addLink(CHAT_ID, request);
        verify(linkListCache, never()).evict(anyLong());
    }

    @Test
    void removeLink_shouldEvictCacheAfterSuccessfulRemove() {
        String url = "https://github.com/owner/repo";

        linkApplicationService.removeLink(CHAT_ID, url);

        verify(linkService).removeLink(CHAT_ID, url);
        verify(linkListCache).evict(CHAT_ID);
    }

    @Test
    void removeLink_shouldNotEvictCache_whenServiceThrows() {
        String url = "https://github.com/owner/repo";
        RuntimeException exception = new RuntimeException("remove failed");

        doThrow(exception).when(linkService).removeLink(CHAT_ID, url);

        assertThatThrownBy(() -> linkApplicationService.removeLink(CHAT_ID, url))
                .isSameAs(exception);

        verify(linkService).removeLink(CHAT_ID, url);
        verify(linkListCache, never()).evict(anyLong());
    }

    private LinkResponse linkResponse(String url) {
        return new LinkResponse(1L, URI.create(url), List.of("java"), OffsetDateTime.now());
    }
}
