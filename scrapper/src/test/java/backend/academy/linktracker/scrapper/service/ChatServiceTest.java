package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.repository.ChatStorage;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    @Mock
    ChatStorage chatStorage;

    @Mock
    LinkStorage linkStorage;

    @InjectMocks
    ChatService chatService;

    @Test
    void register_shouldSave_whenChatNotExists() {
        when(chatStorage.exists(1L)).thenReturn(false);
        chatService.register(1L);
        verify(chatStorage).save(1L);
        verifyNoInteractions(linkStorage);
    }

    @Test
    void register_shouldThrow_whenChatExists() {
        when(chatStorage.exists(1L)).thenReturn(true);
        assertThatThrownBy(() -> chatService.register(1L))
                .isInstanceOf(ChatAlreadyExistsException.class)
                .hasMessageContaining("Чат с id 1 уже зарегистрирован");
        verify(chatStorage, never()).save(anyLong());
    }

    @Test
    void delete_shouldDeleteChatAndItsLinks_whenChatExists() {
        when(chatStorage.exists(1L)).thenReturn(true);
        chatService.delete(1L);
        verify(linkStorage).deleteByChatId(1L);
        verify(chatStorage).delete(1L);
    }

    @Test
    void delete_shouldThrow_whenChatNotExists() {
        when(chatStorage.exists(1L)).thenReturn(false);
        assertThatThrownBy(() -> chatService.delete(1L))
                .isInstanceOf(ChatNotFoundException.class)
                .hasMessageContaining("Чат с id 1 не найден");
        verify(linkStorage, never()).deleteByChatId(anyLong());
        verify(chatStorage, never()).delete(anyLong());
    }
}
