package backend.academy.linktracker.client.impl;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.service.UpdateHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import java.util.List;
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
                        updates.stream()
                                .map(this::convertToUpdateData)
                                .filter(updateData -> updateData != null)
                                .forEach(handler::handle);
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

    @Override
    public void setCommands(List<BotCommandCreation> commands) {
        if (commands == null || commands.isEmpty()) {
            log.warn("Нет команд для отображения в меню");
            return;
        }

        BotCommand[] botCommands = commands.stream()
                .map(cmd -> new BotCommand(cmd.getCommand(), cmd.getDescription()))
                .toArray(BotCommand[]::new);

        var response = telegramBot.execute(new SetMyCommands(botCommands));

        if (response.isOk()) {
            log.info("Успешно установлено {} команд в меню", commands.size());
        } else {
            log.error(
                    "Не удалось установить команды в меню. Код ошибки: {}, описание: {}",
                    response.errorCode(),
                    response.description());
        }
    }

    private UpdateData convertToUpdateData(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return null;
        }

        var message = update.message();
        var chat = message.chat();
        var from = message.from();

        return new UpdateData(
                update.updateId(),
                chat.id(),
                message.text(),
                from != null ? from.id() : null,
                from != null ? from.username() : null);
    }
}
