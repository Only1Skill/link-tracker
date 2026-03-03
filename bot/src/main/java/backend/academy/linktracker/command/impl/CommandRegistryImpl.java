package backend.academy.linktracker.command.impl;

import backend.academy.linktracker.command.BotCommandCreation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Реестр всех команд-стратегий.
 * Использует внедрение всех бинов типа BotCommand через конструктор.
 */
@Slf4j
@Component
public class CommandRegistryImpl implements backend.academy.linktracker.command.CommandRegistry {
    private final Map<String, BotCommandCreation> commands = new HashMap<>();

    public CommandRegistryImpl(Collection<BotCommandCreation> commandsBeans) {
        for (BotCommandCreation command : commandsBeans) {
            commands.put(command.getCommand(), command);
            log.info("Зарегистрированная команда: {} - {}", command.getCommand(), command);
        }
        log.info("Общее количество зарегистрированных команд: {}", commands.size());
    }

    public Optional<BotCommandCreation> getStrategy(String commandText) {
        return Optional.ofNullable(commands.get(commandText));
    }

    public Collection<BotCommandCreation> getAllCommands() {
        return commands.values();
    }
}
