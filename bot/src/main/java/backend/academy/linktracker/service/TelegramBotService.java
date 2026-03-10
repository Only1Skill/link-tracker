package backend.academy.linktracker.service;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.command.CommandRegistry;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.service.state.TrackState;
import backend.academy.linktracker.service.state.UserStateManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService implements UpdateHandler {
    private final TelegramClient telegramClient;
    private final CommandRegistry commandRegistry;
    private final UserStateManager userStateManager;

    private static final String UNKNOWN_COMMAND_RESPONSE =
            "Извините, я не понимаю эту команду. Используйте /help для списка команд.";

    @PostConstruct
    @Profile("!test")
    public void init() {
        telegramClient.startPolling(this);
        log.info("Телеграм бот запущен и слушает обновления");
    }

    public void handle(UpdateData updateData) {
        if (updateData.messageText() == null || updateData.messageText().isEmpty()) {
            return;
        }

        Long chatId = updateData.chatId();
        String text = updateData.messageText();

        TrackState currentState = userStateManager.getState(chatId);

        if (currentState != TrackState.NONE && !text.startsWith("/")) {
            BotCommandCreation trackCommand =
                    commandRegistry.getStrategy("/track").orElse(null);
            if (trackCommand != null) {
                String response = trackCommand.execute(updateData);
                if (response != null) {
                    telegramClient.sendMessage(chatId, response);
                }
            }
            return;
        }

        if (text.equals("/cancel")) {
            userStateManager.clear(chatId);
            telegramClient.sendMessage(chatId, "Диалог отменён.");
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
                            .log("Неизвестная команда получена");
                    return UNKNOWN_COMMAND_RESPONSE;
                });

        if (response != null) {
            telegramClient.sendMessage(updateData.chatId(), response);
        }
    }
}
