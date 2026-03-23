package backend.academy.linktracker.command;

import java.util.Collection;
import java.util.Optional;

public interface CommandRegistry {
    Optional<BotCommandCreation> getStrategy(String commandText);

    Collection<BotCommandCreation> getAllCommands();
}
