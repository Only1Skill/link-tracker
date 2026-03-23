package backend.academy.linktracker.scrapper.exception;

public class ChatAlreadyExistsException extends ScrapperException {
    public ChatAlreadyExistsException(Long chatId) {
        super("Чат с id " + chatId + " уже зарегистрирован");
    }
}
