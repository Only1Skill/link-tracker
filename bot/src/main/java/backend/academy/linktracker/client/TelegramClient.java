package backend.academy.linktracker.client;

import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.service.UpdateHandler;
import java.util.List;

public interface TelegramClient {
    void sendMessage(long chatId, String text);

    void startPolling(UpdateHandler updateHandler);

    void setCommands(List<BotCommandCreation> commands);
}
