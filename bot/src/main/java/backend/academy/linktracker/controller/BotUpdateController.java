package backend.academy.linktracker.controller;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.dto.LinkUpdate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/updates")
@RequiredArgsConstructor
@Slf4j
public class BotUpdateController {
    private final TelegramClient telegramClient;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void sendUpdate(@Valid @RequestBody LinkUpdate update) {
        for (Long chatId : update.tgChatIds()) {
            telegramClient.sendMessage(chatId, update.description());
        }
    }
}
