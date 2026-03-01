package backend.academy.linktracker.port;

public interface TelegramClient {
    void sendMessage(long chatId, String text);

    void startPolling(UpdateHandler updateHandler);
}
