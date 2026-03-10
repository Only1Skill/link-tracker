package backend.academy.linktracker.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.service.CommandExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("StartCommand Unit Tests")
@ExtendWith(MockitoExtension.class)
class StartCommandTest {

    @Mock
    private ScrapperClient scrapperClient;
    @Mock
    private CommandExecutor commandExecutor;

    private StartCommand startCommand;

    @BeforeEach
    void setUp() {
        startCommand = new StartCommand(scrapperClient, commandExecutor);
    }

    @Nested
    @DisplayName("execute Method Tests")
    class ExecuteTests {

        @Test
        @DisplayName("Should return welcome message and register chat")
        void shouldReturnWelcomeMessageAndRegisterChat() {
            // given
            UpdateData updateData = new UpdateData(1, 12345L, "/start", 67890L, "testuser");
            // Заставляем executor выполнить переданное действие без ошибок
            doAnswer(invocation -> {
                Runnable action = invocation.getArgument(0);
                action.run();
                return null;
            }).when(commandExecutor).executeScrapperCallVoid(any(), anyLong(), anyString());

            // when
            String result = startCommand.execute(updateData);

            // then
            assertThat(result)
                    .contains("Добро пожаловать")
                    .contains("отслеживания изменений")
                    .contains("/help");
            verify(commandExecutor).executeScrapperCallVoid(
                    any(Runnable.class),
                    eq(12345L),
                    eq("Не удалось зарегистрировать чат. Попробуйте позже.")
            );
            verify(scrapperClient).registerChat(12345L);
        }

        @Test
        @DisplayName("Should return consistent response regardless of input")
        void shouldReturnConsistentResponseRegardlessOfInput() {
            // given
            UpdateData updateData1 = new UpdateData(1, 123L, "/start", 456L, "user1");
            UpdateData updateData2 = new UpdateData(2, 789L, "/start", 101L, "user2");
            doAnswer(invocation -> {
                Runnable action = invocation.getArgument(0);
                action.run();
                return null;
            }).when(commandExecutor).executeScrapperCallVoid(any(), anyLong(), anyString());

            // when
            String result1 = startCommand.execute(updateData1);
            String result2 = startCommand.execute(updateData2);

            // then
            assertThat(result1).isEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("Command Metadata Tests")
    class CommandMetadataTests {

        @Test
        @DisplayName("Should return correct command string")
        void shouldReturnCorrectCommandString() {
            // when
            String command = startCommand.getCommand();

            // then
            assertThat(command).isEqualTo("/start");
        }

        @Test
        @DisplayName("Should return correct description")
        void shouldReturnCorrectDescription() {
            // when
            String description = startCommand.getDescription();

            // then
            assertThat(description).isEqualTo("Начало работы с ботом");
        }
    }
}
