package backend.academy.linktracker.service;

import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.exception.ScrapperClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommandExecutor {
    private final TelegramClient telegramClient;

    /**
     * Выполняет действие, возвращающее результат. При ошибке отправляет сообщение и возвращает null.
     */
    public <T> T executeScrapperCall(Supplier<T> action, long chatId, String errorMessage) {
        try {
            return action.get();
        } catch (ScrapperClientException e) {
            log.atError()
                .setCause(e)
                .addKeyValue("chatId", chatId)
                .log("Ошибка при вызове Scrapper: {}", e.getMessage());
            telegramClient.sendMessage(chatId, errorMessage);
            return null;
        }
    }

    /**
     * Для методов без возвращаемого значения (void).
     */
    public void executeScrapperCallVoid(Runnable action, long chatId, String errorMessage) {
        try {
            action.run();
        } catch (ScrapperClientException e) {
            log.atError()
                .setCause(e)
                .addKeyValue("chatId", chatId)
                .log("Ошибка при вызове Scrapper: {}", e.getMessage());
            telegramClient.sendMessage(chatId, errorMessage);
        }
    }
}
