package backend.academy.linktracker.command.impl;

import backend.academy.linktracker.command.BotCommand;
import com.pengrad.telegrambot.model.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("unknown")
public class UnknownCommand implements BotCommand {
    private static final String RESPONSE = "Извините, я не понимаю эту команду. Используйте /help для списка команд.";

    @Override
    public String execute(Update update) {
        Long chatId = update.message().chat().id();
        String unknownCommand = update.message().text();
        log.atInfo()
                .addKeyValue("chatId", chatId)
                .addKeyValue("unknownCommand", unknownCommand)
                .log("Unknown command received");
        return RESPONSE;
    }

    @Override
    public String getCommand() {
        return "/unknown";
    }

    @Override
    public String getDescription() {
        return null;
    }
}
