package backend.academy.linktracker.command.impl;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.service.CommandExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("/start")
@RequiredArgsConstructor
public class StartCommand implements BotCommandCreation {
    private final ScrapperClient scrapperClient;
    private final CommandExecutor commandExecutor;

    private static final String RESPONSE = """
        Добро пожаловать! Я бот для отслеживания изменений на сайтах.

        Я буду уведомлять вас, когда на интересующих вас страницах появятся изменения.

        Используйте /help для списка доступных команд.
        """;

    @Override
    public String execute(UpdateData updateData) {
        Long chatId = updateData.chatId();

        commandExecutor.executeScrapperCallVoid(() -> scrapperClient.registerChat(chatId), chatId);

        log.atInfo()
                .addKeyValue("chatId", updateData.chatId())
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
