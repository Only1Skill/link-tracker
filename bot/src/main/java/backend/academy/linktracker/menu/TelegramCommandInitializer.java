package backend.academy.linktracker.menu;

import backend.academy.linktracker.command.CommandRegistry;
import backend.academy.linktracker.port.TelegramClient;
import backend.academy.linktracker.port.dto.CommandInfo;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class TelegramCommandInitializer {

    private final TelegramClient telegramClient;
    private final CommandRegistry commandRegistry;

    @PostConstruct
    public void initMethod() {
        log.info("Настройка меню команд бота...");

        List<CommandInfo> commands = commandRegistry.getAllCommands().stream()
                .map(cmd -> new CommandInfo(cmd.getCommand(), cmd.getDescription()))
                .filter(cmd -> cmd.description() != null && !cmd.description().isEmpty())
                .toList();

        telegramClient.setCommands(commands);
    }
}
