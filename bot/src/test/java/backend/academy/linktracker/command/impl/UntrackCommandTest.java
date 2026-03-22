package backend.academy.linktracker.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.exception.ScrapperClientException;
import backend.academy.linktracker.service.CommandExecutor;
import backend.academy.linktracker.service.state.TrackState;
import backend.academy.linktracker.service.state.UserStateManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UntrackCommandTest {
    @Mock
    ScrapperClient scrapperClient;

    @Mock
    UserStateManager userStateManager;

    @Mock
    CommandExecutor commandExecutor;

    @InjectMocks
    UntrackCommand untrackCommand;

    @Test
    void whenStateNone_setsAwaitingUntrackLinkAndReturnsPrompt() {
        UpdateData update = new UpdateData(1, 123L, "/untrack", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.NONE);
        String result = untrackCommand.execute(update);
        assertThat(result).isEqualTo("Отправьте ссылку, которую хотите перестать отслеживать:");
        verify(userStateManager).setState(123L, TrackState.AWAITING_UNTRACK_LINK);
    }

    @Test
    void whenAwaitingUntrackLinkAndValidUrl_deletesAndClears() {
        UpdateData update = new UpdateData(1, 123L, "https://github.com/user/repo", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.AWAITING_UNTRACK_LINK);
        doAnswer(inv -> {
                    inv.getArgument(0, Runnable.class).run();
                    return null;
                })
                .when(commandExecutor)
                .executeScrapperCallVoid(any(Runnable.class), eq(123L));

        String result = untrackCommand.execute(update);
        assertThat(result).isEqualTo("Ссылка успешно удалена из отслеживаемых!");
        verify(userStateManager).clear(123L);
        verify(scrapperClient).removeLink(123L, "https://github.com/user/repo");
    }

    @Test
    void whenAwaitingUntrackLinkAndInvalidUrl_returnsError() {
        UpdateData update = new UpdateData(1, 123L, "not a url", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.AWAITING_UNTRACK_LINK);
        String result = untrackCommand.execute(update);
        assertThat(result).isEqualTo("Некорректная ссылка. Попробуйте ещё раз или используйте /cancel для отмены.");
        verify(userStateManager, never()).clear(any());
        verify(scrapperClient, never()).removeLink(anyLong(), any());
    }

    @Test
    void whenAwaitingUntrackLinkAndScrapperThrows_returnsErrorAndClears() {
        UpdateData update = new UpdateData(1, 123L, "https://github.com/user/repo", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.AWAITING_UNTRACK_LINK);
        doThrow(new ScrapperClientException("Link not found"))
                .when(commandExecutor)
                .executeScrapperCallVoid(any(Runnable.class), eq(123L));

        String result = untrackCommand.execute(update);
        assertThat(result).isEqualTo("Link not found");
        verify(userStateManager).clear(123L);
    }
}
