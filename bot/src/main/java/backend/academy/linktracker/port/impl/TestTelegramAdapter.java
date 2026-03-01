package backend.academy.linktracker.port.impl;

import backend.academy.linktracker.port.TelegramClient;
import backend.academy.linktracker.port.UpdateHandler;
import backend.academy.linktracker.port.dto.CommandInfo;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestTelegramAdapter implements TelegramClient {

    @Override
    public void sendMessage(long chatId, String text) {
        // ничего не делаем в тестах
    }

    @Override
    public void startPolling(UpdateHandler handler) {
        // ничего не делаем в тестах
    }

    @Override
    public void setCommands(List<CommandInfo> commands) {
        // ничего не делаем в тестах
    }
}
