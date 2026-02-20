package backend.academy.linktracker.service;

import backend.academy.linktracker.handler.UserCommandHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {
    private final TelegramBot telegramBot;
    private final UserCommandHandler userCommandHandler;

    Logger logger = LoggerFactory.getLogger(TelegramBotService.class);

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(updates -> {
            try {
                updates.forEach(this::processUpdate);
            } catch (Exception e) {
                log.error("On TELEGRAM Updates error {}", e.toString());
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> {
            if (e.response() != null) {
                log.warn("TELEGRAM ERR: {} - {}", e.response().errorCode(), e.response().description());
            } else {
                e.printStackTrace();
            }
        });
        logger.info("Телеграм бот запущен и слушает обновления");
    }

    void processUpdate(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return;
        }

        long chatId = update.message().chat().id();
        String messageText = update.message().text();

        log.info("Received message: chatId{}, text={}, length={}", chatId, messageText, messageText.length());

        String response;
        if ("/start".equals(messageText)) {
            response = userCommandHandler.handleStart();
        } else if ("/help".equals(messageText)) {
            response = userCommandHandler.handleHelp();
        } else {
            response = userCommandHandler.handleUnknown();
        }
        sendMessage(chatId, response);
    }

    private void sendMessage(long chatId, String message) {
        telegramBot.execute(new SendMessage(chatId, message));
    }
}
