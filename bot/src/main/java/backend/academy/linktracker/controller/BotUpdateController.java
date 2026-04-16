package backend.academy.linktracker.controller;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.dto.ChatNotification;
import backend.academy.linktracker.dto.LinkUpdate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BotUpdateController {

    private final TelegramClient telegramClient;

    @PostMapping("/updates")
    @ResponseStatus(HttpStatus.OK)
    public void sendUpdate(@Valid @RequestBody LinkUpdate update) {
        for (Long chatId : update.tgChatIds()) {
            telegramClient.sendMessage(chatId, update.description());
        }
    }

    @PostMapping("/notifications")
    @ResponseStatus(HttpStatus.OK)
    public void sendNotification(@Valid @RequestBody ChatNotification notification) {
        for (Long chatId : notification.tgChatIds()) {
            telegramClient.sendMessage(chatId, notification.message());
        }
    }
}
