package backend.academy.linktracker.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.command.impl.CommandRegistryImpl;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CommandRegistryImpl Unit Tests")
class CommandRegistryImplTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create empty registry with empty command list")
        void shouldCreateEmptyRegistryWithEmptyCommandList() {
            // given
            List<BotCommandCreation> emptyCommands = List.of();

            // when
            CommandRegistry registry = new CommandRegistryImpl(emptyCommands);

            // then
            assertThat(registry.getAllCommands()).isEmpty();
        }

        @Test
        @DisplayName("Should register all provided commands")
        void shouldRegisterAllProvidedCommands() {
            // given
            BotCommandCreation startCommand = mock(BotCommandCreation.class);
            when(startCommand.getCommand()).thenReturn("/start");

            BotCommandCreation helpCommand = mock(BotCommandCreation.class);
            when(helpCommand.getCommand()).thenReturn("/help");

            List<BotCommandCreation> commands = List.of(startCommand, helpCommand);

            // when
            CommandRegistry registry = new CommandRegistryImpl(commands);

            // then
            assertThat(registry.getAllCommands()).hasSize(2).containsExactlyInAnyOrder(startCommand, helpCommand);
        }

        @Test
        @DisplayName("Should override duplicate command with same key")
        void shouldOverrideDuplicateCommandWithSameKey() {
            // given
            BotCommandCreation startCommand1 = mock(BotCommandCreation.class);
            when(startCommand1.getCommand()).thenReturn("/start");
            when(startCommand1.getDescription()).thenReturn("Original");

            BotCommandCreation startCommand2 = mock(BotCommandCreation.class);
            when(startCommand2.getCommand()).thenReturn("/start");
            when(startCommand2.getDescription()).thenReturn("Modified");

            List<BotCommandCreation> commands = List.of(startCommand1, startCommand2);

            // when
            CommandRegistry registry = new CommandRegistryImpl(commands);

            // then
            assertThat(registry.getAllCommands()).hasSize(1);
            assertThat(registry.getStrategy("/start").get().getDescription()).isEqualTo("Modified");
        }
    }

    @Nested
    @DisplayName("getStrategy Method Tests")
    class GetStrategyTests {

        private CommandRegistry registry;
        private BotCommandCreation startCommand;
        private BotCommandCreation helpCommand;

        @BeforeEach
        void setUp() {
            startCommand = mock(BotCommandCreation.class);
            when(startCommand.getCommand()).thenReturn("/start");

            helpCommand = mock(BotCommandCreation.class);
            when(helpCommand.getCommand()).thenReturn("/help");

            registry = new CommandRegistryImpl(List.of(startCommand, helpCommand));
        }

        @Test
        @DisplayName("Should return command by exact key match")
        void shouldReturnCommandByExactKeyMatch() {
            // when
            Optional<BotCommandCreation> result = registry.getStrategy("/start");

            // then
            assertThat(result).isPresent().contains(startCommand);
        }

        @Test
        @DisplayName("Should return empty Optional for non-existent command")
        void shouldReturnEmptyOptionalForNonExistentCommand() {
            // when
            Optional<BotCommandCreation> result = registry.getStrategy("/nonexistent");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle null command key")
        void shouldHandleNullCommandKey() {
            // when
            Optional<BotCommandCreation> result = registry.getStrategy(null);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllCommands Method Tests")
    class GetAllCommandsTests {

        @Test
        @DisplayName("Should return all registered commands")
        void shouldReturnAllRegisteredCommands() {
            // given
            BotCommandCreation startCommand = mock(BotCommandCreation.class);
            when(startCommand.getCommand()).thenReturn("/start");

            BotCommandCreation helpCommand = mock(BotCommandCreation.class);
            when(helpCommand.getCommand()).thenReturn("/help");

            CommandRegistry registry = new CommandRegistryImpl(List.of(startCommand, helpCommand));

            // when
            Collection<BotCommandCreation> allCommands = registry.getAllCommands();

            // then
            assertThat(allCommands).hasSize(2).containsExactlyInAnyOrder(startCommand, helpCommand);
        }

        @Test
        @DisplayName("Should return empty collection for empty registry")
        void shouldReturnEmptyCollectionForEmptyRegistry() {
            // given
            CommandRegistry registry = new CommandRegistryImpl(List.of());

            // when
            Collection<BotCommandCreation> allCommands = registry.getAllCommands();

            // then
            assertThat(allCommands).isEmpty();
        }

        @Test
        @DisplayName("Should return unmodifiable collection")
        void shouldReturnUnmodifiableCollection() {
            // given
            BotCommandCreation startCommand = mock(BotCommandCreation.class);
            when(startCommand.getCommand()).thenReturn("/start");
            CommandRegistry registry = new CommandRegistryImpl(List.of(startCommand));

            // when
            Collection<BotCommandCreation> allCommands = registry.getAllCommands();

            // then
            assertThatThrownBy(() -> allCommands.add(mock(BotCommandCreation.class)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
