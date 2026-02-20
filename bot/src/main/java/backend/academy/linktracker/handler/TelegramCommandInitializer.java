package backend.academy.linktracker.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramCommandInitializer {

    private final TelegramBot telegramBot;

    @PostConstruct
    public void initMethod() {
        log.info("Настройка меню команд бота...");

        BotCommand[] commands = {
            new BotCommand("/start", "Начало работы c ботом"),
            new BotCommand("/help", "Показать список доступных команд")
        };

        SetMyCommands setMyCommands = new SetMyCommands(commands);

        var response = telegramBot.execute(setMyCommands);

        if (response.isOk()) {
            log.info("Успешная установка команд в меню!");
        } else {
            log.info("Не удалось установить команды в меню. Код ошибки: {}, описание: {}", response.errorCode(), response.description());
        }
    }
}
