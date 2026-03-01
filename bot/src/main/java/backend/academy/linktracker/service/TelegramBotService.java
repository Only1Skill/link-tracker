package backend.academy.linktracker.service;

import backend.academy.linktracker.command.BotCommand;
import backend.academy.linktracker.command.CommandRegistry;
import backend.academy.linktracker.port.TelegramClient;
import backend.academy.linktracker.port.UpdateHandler;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService implements UpdateHandler {
    private final TelegramClient telegramClient;
    private final CommandRegistry commandRegistry;

    @PostConstruct
    public void init() {
        telegramClient.startPolling(this);
        log.info("Телеграм бот запущен и слушает обновления");
    }

    public void handle(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return;
        }

        long chatId = update.message().chat().id();
        String messageText = update.message().text();

        log.atInfo()
                .addKeyValue("chatId", chatId)
                .addKeyValue("text", messageText)
                .addKeyValue("length", messageText.length())
                .addKeyValue("userId", update.message().from().id())
                .log("Получено сообщение");

        String[] parts = messageText.split("\\s+", 2);
        String commandKey = parts[0].toLowerCase();

        BotCommand strategy = commandRegistry.getStrategy(commandKey);
        String response = strategy.execute(update);

        telegramClient.sendMessage(chatId, response);
    }
}
