package backend.academy.linktracker.service;

import backend.academy.linktracker.command.CommandRegistry;
import backend.academy.linktracker.port.TelegramClient;
import backend.academy.linktracker.port.UpdateHandler;
import backend.academy.linktracker.port.dto.UpdateData;
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

    private static final String UNKNOWN_COMMAND_RESPONSE =
            "Извините, я не понимаю эту команду. Используйте /help для списка команд.";

    @PostConstruct
    public void init() {
        telegramClient.startPolling(this);
        log.info("Телеграм бот запущен и слушает обновления");
    }

    public void handle(UpdateData updateData) {
        if (updateData.messageText() == null || updateData.messageText().isEmpty()) {
            return;
        }

        log.atInfo()
                .addKeyValue("chatId", updateData.chatId())
                .addKeyValue("text", updateData.messageText())
                .addKeyValue("userId", updateData.userId())
                .log("Обработка сообщения");

        String[] parts = updateData.messageText().split("\\s+", 2);
        String commandKey = parts[0].toLowerCase();

        String response = commandRegistry
                .getStrategy(commandKey)
                .map(cmd -> cmd.execute(updateData))
                .orElseGet(() -> {
                    log.atInfo()
                            .addKeyValue("chatId", updateData.chatId())
                            .addKeyValue("unknownCommand", updateData.messageText())
                            .log("Unknown command received");
                    return UNKNOWN_COMMAND_RESPONSE;
                });

        telegramClient.sendMessage(updateData.chatId(), response);
    }
}
