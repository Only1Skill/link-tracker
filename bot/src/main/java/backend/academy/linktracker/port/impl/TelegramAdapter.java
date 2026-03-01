package backend.academy.linktracker.port.impl;

import backend.academy.linktracker.port.TelegramClient;
import backend.academy.linktracker.port.UpdateHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramAdapter implements TelegramClient {
    private final TelegramBot telegramBot;

    @Override
    public void sendMessage(long chatId, String text) {
        telegramBot.execute(new SendMessage(chatId, text));
    }

    @Override
    public void startPolling(UpdateHandler handler) {
        telegramBot.setUpdatesListener(
                updates -> {
                    try {
                        updates.forEach(handler::handle);
                    } catch (Exception e) {
                        log.atError()
                                .setCause(e)
                                .addKeyValue("updatesCount", updates.size())
                                .log("Ошибка в получении обновлений");
                    }
                    return UpdatesListener.CONFIRMED_UPDATES_ALL;
                },
                e -> {
                    if (e.response() != null) {
                        log.atWarn()
                                .addKeyValue("errorCode", e.response().errorCode())
                                .addKeyValue("description", e.response().description())
                                .log("Ошибка на стороне Telegram");
                    } else {
                        log.error("Ошибка бота", e);
                    }
                });
    }
}
