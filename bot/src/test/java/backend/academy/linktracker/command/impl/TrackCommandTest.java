package backend.academy.linktracker.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.exception.ScrapperClientException;
import backend.academy.linktracker.service.CommandExecutor;
import backend.academy.linktracker.service.state.TrackState;
import backend.academy.linktracker.service.state.UserStateManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackCommandTest {

    @Mock
    private CommandExecutor commandExecutor;

    @Mock
    private UserStateManager userStateManager;

    @Mock
    private ScrapperClient scrapperClient;

    @InjectMocks
    private TrackCommand trackCommand;

    @Test
    void whenNoState_setsAwaitingLinkAndReturnsPrompt() {
        UpdateData update = new UpdateData(1, 123L, "/track", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.NONE);

        String result = trackCommand.execute(update);

        assertThat(result).isEqualTo("Отправьте ссылку, которую хотите отслеживать, либо /cancel для отмены диалога");
        verify(userStateManager).setState(123L, TrackState.AWAITING_LINK);
    }

    @Test
    void whenAwaitingLinkAndValidUrl_movesToAwaitingTags() {
        UpdateData update = new UpdateData(1, 123L, "https://github.com/owner/repo", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.AWAITING_LINK);

        String result = trackCommand.execute(update);

        assertThat(result).isEqualTo("Теперь укажите теги через запятую или отправьте 'пропустить':");
        verify(userStateManager).setLink(123L, "https://github.com/owner/repo");
        verify(userStateManager).setState(123L, TrackState.AWAITING_TAGS);
    }

    @Test
    void whenAwaitingLinkAndInvalidUrl_returnsErrorMessage() {
        UpdateData update = new UpdateData(1, 123L, "invalid", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.AWAITING_LINK);

        String result = trackCommand.execute(update);

        assertThat(result).isEqualTo("Некорректная ссылка. Попробуйте еще раз");
        verify(userStateManager, never()).setLink(anyLong(), anyString());
    }

    @Test
    void whenAwaitingTagsAndScrapperSuccess_clearsStateAndReturnsSuccess() {
        UpdateData update = new UpdateData(1, 123L, "java,spring", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.AWAITING_TAGS);
        when(userStateManager.getLink(123L)).thenReturn("https://github.com/owner/repo");

        doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run();
                    return null;
                })
                .when(commandExecutor)
                .executeScrapperCallVoid(any(Runnable.class), eq(123L));

        String result = trackCommand.execute(update);

        assertThat(result).isEqualTo("Ссылка успешно добавлена!");
        verify(userStateManager).clear(123L);
        verify(scrapperClient)
                .addLink(
                        eq(123L),
                        argThat(req -> req.link().equals("https://github.com/owner/repo")
                                && req.tags().equals(List.of("java", "spring"))));
    }

    @Test
    void whenAwaitingTagsAndScrapperThrows_returnsErrorAndClears() {
        UpdateData update = new UpdateData(1, 123L, "java", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.AWAITING_TAGS);
        when(userStateManager.getLink(123L)).thenReturn("https://github.com/owner/repo");

        doThrow(new ScrapperClientException("Already tracked"))
                .when(commandExecutor)
                .executeScrapperCallVoid(any(Runnable.class), eq(123L));

        String result = trackCommand.execute(update);

        assertThat(result).isEqualTo("Already tracked");
        verify(userStateManager).clear(123L);
    }
}
