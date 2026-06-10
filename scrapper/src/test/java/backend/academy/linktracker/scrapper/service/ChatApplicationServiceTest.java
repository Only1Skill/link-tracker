package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import backend.academy.linktracker.scrapper.cache.LinkListCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatApplicationServiceTest {
    private static final Long CHAT_ID = 123L;

    @Mock
    private ChatService chatService;

    @Mock
    private LinkListCache linkListCache;

    @InjectMocks
    private ChatApplicationService chatApplicationService;

    @Test
    void register_shouldDelegateToChatServiceAndNotTouchCache() {
        chatApplicationService.register(CHAT_ID);

        verify(chatService).register(CHAT_ID);
        verifyNoInteractions(linkListCache);
    }

    @Test
    void delete_shouldEvictCacheAfterSuccessfulDelete() {
        chatApplicationService.delete(CHAT_ID);

        verify(chatService).delete(CHAT_ID);
        verify(linkListCache).evict(CHAT_ID);
    }

    @Test
    void delete_shouldNotEvictCache_whenServiceThrows() {
        RuntimeException exception = new RuntimeException("delete failed");

        doThrow(exception).when(chatService).delete(CHAT_ID);

        assertThatThrownBy(() -> chatApplicationService.delete(CHAT_ID)).isSameAs(exception);

        verify(chatService).delete(CHAT_ID);
        verify(linkListCache, never()).evict(anyLong());
    }
}
