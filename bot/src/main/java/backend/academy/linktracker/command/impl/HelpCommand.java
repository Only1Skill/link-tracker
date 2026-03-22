package backend.academy.linktracker.command.impl;

import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.command.CommandRegistry;
import backend.academy.linktracker.dto.UpdateData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component("/help")
public class HelpCommand implements BotCommandCreation {
    private final CommandRegistry commandRegistry;

    public HelpCommand(@Lazy CommandRegistryImpl commandRegistryImpl) {
        this.commandRegistry = commandRegistryImpl;
    }

    @Override
    public String execute(UpdateData updateData) {
        log.atInfo()
                .addKeyValue("chatId", updateData.chatId())
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
