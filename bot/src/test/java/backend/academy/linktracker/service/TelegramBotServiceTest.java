package backend.academy.linktracker.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.command.CommandRegistry;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.exception.ScrapperClientException;
import backend.academy.linktracker.service.state.TrackState;
import backend.academy.linktracker.service.state.UserStateManager;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
class TelegramBotServiceTest {

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private CommandRegistry commandRegistry;

    @Mock
    private UserStateManager userStateManager;

    @InjectMocks
    private TelegramBotService botService;

    @Test
    void handlesUnknownCommand() {
        UpdateData update = new UpdateData(1, 123L, "/unknown", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.NONE);
        when(commandRegistry.getStrategy("/unknown")).thenReturn(Optional.empty());

        botService.handle(update);

        verify(telegramClient)
                .sendMessage(123L, "Извините, я не понимаю эту команду. Используйте /help для списка команд.");
    }

    @Test
    void executesCommandAndSendsResponse() {
        UpdateData update = new UpdateData(1, 123L, "/help", null, null);
        BotCommandCreation command = mock(BotCommandCreation.class);
        when(userStateManager.getState(123L)).thenReturn(TrackState.NONE);
        when(commandRegistry.getStrategy("/help")).thenReturn(Optional.of(command));
        when(command.execute(update)).thenReturn("Help text");

        botService.handle(update);

        verify(telegramClient).sendMessage(123L, "Help text");
    }

    @Test
    void sendsErrorMessage_whenCommandThrowsScrapperException() {
        UpdateData update = new UpdateData(1, 123L, "/start", null, null);
        BotCommandCreation command = mock(BotCommandCreation.class);
        when(userStateManager.getState(123L)).thenReturn(TrackState.NONE);
        when(commandRegistry.getStrategy("/start")).thenReturn(Optional.of(command));
        when(command.execute(update)).thenThrow(new ScrapperClientException("Chat exists"));

        botService.handle(update);

        verify(telegramClient).sendMessage(123L, "Chat exists");
    }

    @Test
    void sendsErrorMessage_whenResourceAccessException() {
        UpdateData update = new UpdateData(1, 123L, "/start", null, null);
        BotCommandCreation command = mock(BotCommandCreation.class);
        when(userStateManager.getState(123L)).thenReturn(TrackState.NONE);
        when(commandRegistry.getStrategy("/start")).thenReturn(Optional.of(command));
        when(command.execute(update)).thenThrow(new ResourceAccessException("Network error"));

        botService.handle(update);

        verify(telegramClient).sendMessage(123L, "Сервис временно недоступен. Попробуйте позже.");
    }

    @Test
    void handlesCancelCommand() {
        UpdateData update = new UpdateData(1, 123L, "/cancel", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.AWAITING_LINK);

        botService.handle(update);

        verify(userStateManager).clear(123L);
        verify(telegramClient).sendMessage(123L, "Диалог отменён.");
    }

    @Test
    void handlesAwaitingTrackLinkState() {
        UpdateData update = new UpdateData(1, 123L, "https://github.com/owner/repo", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.AWAITING_LINK);
        BotCommandCreation trackCommand = mock(BotCommandCreation.class);
        when(commandRegistry.getStrategy("/track")).thenReturn(Optional.of(trackCommand));
        when(trackCommand.execute(update)).thenReturn("Processing");

        botService.handle(update);

        verify(telegramClient).sendMessage(123L, "Processing");
    }

    @Test
    void handlesAwaitingUntrackLinkState() {
        UpdateData update = new UpdateData(1, 123L, "https://github.com/owner/repo", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.AWAITING_UNTRACK_LINK);
        BotCommandCreation untrackCommand = mock(BotCommandCreation.class);
        when(commandRegistry.getStrategy("/track")).thenReturn(Optional.empty());
        when(commandRegistry.getStrategy("/untrack")).thenReturn(Optional.of(untrackCommand));
        when(untrackCommand.execute(update)).thenReturn("Deleted");

        botService.handle(update);

        verify(telegramClient).sendMessage(123L, "Deleted");
    }

    @Test
    void doesNotCrashWhenTrackCommandNotFoundInAwaitingState() {
        UpdateData update = new UpdateData(1, 123L, "some text", null, null);
        when(userStateManager.getState(123L)).thenReturn(TrackState.AWAITING_LINK);
        when(commandRegistry.getStrategy("/track")).thenReturn(Optional.empty());

        botService.handle(update);

        verify(telegramClient, never()).sendMessage(anyLong(), anyString());
    }
}
