package backend.academy.linktracker.port;

import backend.academy.linktracker.port.dto.CommandInfo;
import java.util.List;

public interface TelegramClient {
    void sendMessage(long chatId, String text);

    void startPolling(UpdateHandler updateHandler);

    void setCommands(List<CommandInfo> commands);
}
