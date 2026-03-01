package backend.academy.linktracker.command;

import backend.academy.linktracker.command.impl.UnknownCommand;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Реестр всех команд-стратегий.
 * Использует внедрение всех бинов типа BotCommand через конструктор.
 */
@Slf4j
@Component
public class CommandRegistry {
    private final Map<String, BotCommand> commands = new HashMap<>();
    private final UnknownCommand unknownCommand;

    public CommandRegistry(Collection<BotCommand> commandsBeans, UnknownCommand unknownCommand) {
        this.unknownCommand = unknownCommand;

        for (BotCommand command : commandsBeans) {
            if (!(command instanceof UnknownCommand)) {
                commands.put(command.getCommand(), command);
                log.info("Зарегистрированная команда: {} - {}", command.getCommand(), command);
            }
        }
        log.info("Общее количество зарегистрированных команд: {}", commands.size());
    }

    public BotCommand getStrategy(String commandText) {
        return commands.getOrDefault(commandText, unknownCommand);
    }

    public Collection<BotCommand> getAllCommands() {
        return commands.values();
    }
}
