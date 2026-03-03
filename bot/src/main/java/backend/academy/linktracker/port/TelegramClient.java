package backend.academy.linktracker.port;

import backend.academy.linktracker.command.BotCommandCreation;
import java.util.List;

public interface TelegramClient {
    void sendMessage(long chatId, String text);

    void startPolling(UpdateHandler updateHandler);

    void setCommands(List<BotCommandCreation> commands);
}
