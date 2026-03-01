package backend.academy.linktracker.command.impl;

import backend.academy.linktracker.command.BotCommand;
import com.pengrad.telegrambot.model.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("/start")
public class StartCommand implements BotCommand {
    private static final String RESPONSE = """
        Добро пожаловать! Я бот для отслеживания изменений на сайтах.

        Я буду уведомлять вас, когда на интересующих вас страницах появятся изменения.

        Используйте /help для списка доступных команд.
        """;

    @Override
    public String execute(Update update) {
        Long chatId = update.message().chat().id();
        log.atInfo()
                .addKeyValue("chatId", chatId)
                .addKeyValue("getCommand", getCommand())
                .log("Инициализация стартовой команды");
        return RESPONSE;
    }

    @Override
    public String getCommand() {
        return "/start";
    }

    @Override
    public String getDescription() {
        return "Начало работы с ботом";
    }
}
