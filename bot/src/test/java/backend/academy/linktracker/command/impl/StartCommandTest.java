package backend.academy.linktracker.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.service.CommandExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StartCommandTest {
    @Mock
    ScrapperClient scrapperClient;

    @Mock
    CommandExecutor commandExecutor;

    @InjectMocks
    StartCommand startCommand;

    @Test
    void execute_registersChatAndReturnsWelcomeMessage() {
        UpdateData update = new UpdateData(1, 123L, "/start", null, null);
        doAnswer(inv -> {
                    inv.getArgument(0, Runnable.class).run();
                    return null;
                })
                .when(commandExecutor)
                .executeScrapperCallVoid(any(Runnable.class), eq(123L));

        String result = startCommand.execute(update);
        assertThat(result).contains("Добро пожаловать");
        verify(scrapperClient).registerChat(123L);
    }
}
