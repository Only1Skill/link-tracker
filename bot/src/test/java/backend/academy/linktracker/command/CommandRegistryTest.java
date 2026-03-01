package backend.academy.linktracker.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.command.impl.UnknownCommand;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommandRegistryTest {

    @Mock
    private BotCommand command1;

    @Mock
    private BotCommand command2;

    @Mock
    private UnknownCommand unknownCommand;

    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        when(command1.getCommand()).thenReturn("/cmd1");
        when(command2.getCommand()).thenReturn("/cmd2");
        when(unknownCommand.getCommand()).thenReturn("/unknown");

        Collection<BotCommand> commandsBeans = List.of(command1, command2, unknownCommand);
        registry = new CommandRegistry(commandsBeans, unknownCommand);
    }

    @Test
    void shouldRegisterOnlyNonUnknownCommands() {
        assertThat(registry.getStrategy("/cmd1")).isSameAs(command1);
        assertThat(registry.getStrategy("/cmd2")).isSameAs(command2);
        assertThat(registry.getStrategy("/unknown")).isSameAs(unknownCommand);
    }

    @Test
    void shouldReturnUnknownForMissingCommand() {
        BotCommand strategy = registry.getStrategy("/missing");
        assertThat(strategy).isSameAs(unknownCommand);
    }

    @Test
    void getAllCommandsShouldExcludeUnknown() {
        Collection<BotCommand> all = registry.getAllCommands();
        assertThat(all).containsExactlyInAnyOrder(command1, command2);
    }
}
