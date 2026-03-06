package backend.academy.linktracker.menu;

import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.command.CommandRegistry;
import backend.academy.linktracker.port.TelegramClient;
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

        List<BotCommandCreation> commands = commandRegistry.getAllCommands().stream()
                .filter(cmd ->
                        cmd.getDescription() != null && !cmd.getDescription().isEmpty())
                .toList();

        telegramClient.setCommands(commands);
    }
}
