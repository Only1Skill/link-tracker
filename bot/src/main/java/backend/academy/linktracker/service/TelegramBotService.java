package backend.academy.linktracker.service;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.command.CommandRegistry;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.exception.ScrapperClientException;
import backend.academy.linktracker.service.state.TrackState;
import backend.academy.linktracker.service.state.UserStateManager;
import jakarta.annotation.PostConstruct;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService implements UpdateHandler {
    private final TelegramClient telegramClient;
    private final CommandRegistry commandRegistry;
    private final UserStateManager userStateManager;
    private final Environment environment;

    private static final String UNKNOWN_COMMAND_RESPONSE =
            "Извините, я не понимаю эту команду. Используйте /help для списка команд.";

    @PostConstruct
    public void init() {
        if (!environment.matchesProfiles("test")) {
            telegramClient.startPolling(this);
            log.info("Телеграм бот запущен и слушает обновления");
        }
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
            BotCommandCreation untrackCommand =
                    commandRegistry.getStrategy("/untrack").orElse(null);
            if (currentState == TrackState.AWAITING_LINK || currentState == TrackState.AWAITING_TAGS) {
                if (trackCommand != null) {
                    try {
                        String response = trackCommand.execute(updateData);
                        if (response != null) {
                            telegramClient.sendMessage(chatId, response);
                        }
                    } catch (ScrapperClientException | ResourceAccessException e) {
                        String userMessage = determineUserMessage(e);
                        telegramClient.sendMessage(chatId, userMessage);
                    }
                }
            } else if (currentState == TrackState.AWAITING_UNTRACK_LINK) {
                if (untrackCommand != null) {
                    try {
                        String response = untrackCommand.execute(updateData);
                        if (response != null) {
                            telegramClient.sendMessage(chatId, response);
                        }
                    } catch (ScrapperClientException | ResourceAccessException e) {
                        telegramClient.sendMessage(chatId, determineUserMessage(e));
                    }
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

        String[] parts = text.split("\\s+", 2);
        String commandKey = parts[0].toLowerCase();

        Optional<BotCommandCreation> commandOpt = commandRegistry.getStrategy(commandKey);
        if (commandOpt.isPresent()) {
            BotCommandCreation command = commandOpt.get();
            try {
                String response = command.execute(updateData);
                if (response != null) {
                    telegramClient.sendMessage(chatId, response);
                }
            } catch (ScrapperClientException | ResourceAccessException e) {
                String userMessage = determineUserMessage(e);
                telegramClient.sendMessage(chatId, userMessage);
            }
        } else {
            log.atInfo()
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("unknownCommand", text)
                    .log("Неизвестная команда получена");
            telegramClient.sendMessage(chatId, UNKNOWN_COMMAND_RESPONSE);
        }
    }

    private String determineUserMessage(Exception e) {
        if (e instanceof ScrapperClientException scEx) {
            return scEx.getMessage();
        }
        if (e instanceof ResourceAccessException) {
            return "Сервис временно недоступен. Попробуйте позже.";
        }
        return "Произошла ошибка. Попробуйте позже.";
    }
}
