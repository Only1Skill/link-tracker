package backend.academy.linktracker.command.impl;

import backend.academy.linktracker.command.BotCommand;
import backend.academy.linktracker.command.CommandRegistry;
import com.pengrad.telegrambot.model.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component("/help")
public class HelpCommand implements BotCommand {
    private final CommandRegistry commandRegistry;

    public HelpCommand(@Lazy CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    @Override
    public String execute(Update update) {
        Long chatId = update.message().chat().id();
        log.atInfo()
                .addKeyValue("chatId", chatId)
                .addKeyValue("getCommand", getCommand())
                .log("Инициализация команды help");

        StringBuilder helpText = new StringBuilder("Доступные команды:\n\n");
        commandRegistry.getAllCommands().forEach(cmd -> helpText.append(cmd.getCommand())
                .append(" - ")
                .append(cmd.getDescription())
                .append("\n"));

        return helpText.toString();
    }

    @Override
    public String getCommand() {
        return "/help";
    }

    @Override
    public String getDescription() {
        return "Показать список доступных команд";
    }
}
