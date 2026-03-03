package backend.academy.linktracker.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.port.dto.UpdateData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HelpCommand Unit Tests")
class HelpCommandTest {

    private CommandRegistryImpl commandRegistry;
    private HelpCommand helpCommand;

    @BeforeEach
    void setUp() {
        commandRegistry = mock(CommandRegistryImpl.class);
        helpCommand = new HelpCommand(commandRegistry);
    }

    @Nested
    @DisplayName("execute Method Tests")
    class ExecuteTests {

        @Test
        @DisplayName("Should return list of all commands with descriptions")
        void shouldReturnListOfAllCommandsWithDescriptions() {
            // given
            BotCommandCreation startCommand = new StartCommand();
            BotCommandCreation helpCommand2 = new HelpCommand(null);

            when(commandRegistry.getAllCommands()).thenReturn(List.of(startCommand, helpCommand2));

            UpdateData updateData = new UpdateData(1, 12345L, "/help", 67890L, "testuser");

            // when
            String result = helpCommand.execute(updateData);

            // then
            assertThat(result)
                    .contains("/start")
                    .contains("Начало работы с ботом")
                    .contains("/help")
                    .contains("Показать список доступных команд");
        }

        @Test
        @DisplayName("Should handle empty command registry")
        void shouldHandleEmptyCommandRegistry() {
            // given
            when(commandRegistry.getAllCommands()).thenReturn(List.of());

            UpdateData updateData = new UpdateData(1, 12345L, "/help", 67890L, "testuser");

            // when
            String result = helpCommand.execute(updateData);

            // then
            assertThat(result).contains("Доступные команды:");
        }

        @Test
        @DisplayName("Should handle commands with null descriptions")
        void shouldHandleCommandsWithNullDescriptions() {
            // given
            BotCommandCreation commandWithNullDesc = new BotCommandCreation() {
                @Override
                public String execute(UpdateData updateData) {
                    return "test";
                }

                @Override
                public String getCommand() {
                    return "/test";
                }

                @Override
                public String getDescription() {
                    return null;
                }
            };

            when(commandRegistry.getAllCommands()).thenReturn(List.of(commandWithNullDesc));

            UpdateData updateData = new UpdateData(1, 12345L, "/help", 67890L, "testuser");

            // when
            String result = helpCommand.execute(updateData);

            // then
            assertThat(result).contains("/test - null");
        }

        @Test
        @DisplayName("Should format help text properly with newlines")
        void shouldFormatHelpTextProperlyWithNewlines() {
            // given
            when(commandRegistry.getAllCommands()).thenReturn(List.of(new StartCommand()));

            UpdateData updateData = new UpdateData(1, 12345L, "/help", 67890L, "testuser");

            // when
            String result = helpCommand.execute(updateData);

            // then
            assertThat(result).startsWith("Доступные команды:\n\n").contains("\n/start - Начало работы с ботом\n");
        }
    }

    @Nested
    @DisplayName("Command Metadata Tests")
    class CommandMetadataTests {

        @Test
        @DisplayName("Should return correct command string")
        void shouldReturnCorrectCommandString() {
            // when
            String command = helpCommand.getCommand();

            // then
            assertThat(command).isEqualTo("/help");
        }

        @Test
        @DisplayName("Should return correct description")
        void shouldReturnCorrectDescription() {
            // when
            String description = helpCommand.getDescription();

            // then
            assertThat(description).isEqualTo("Показать список доступных команд");
        }
    }
}
