package backend.academy.linktracker.scrapper.exception;

public class ChatNotFoundException extends ScrapperException {
    public ChatNotFoundException(Long chatId) {
        super("Чат с id " + chatId + " не найден");
    }
}
