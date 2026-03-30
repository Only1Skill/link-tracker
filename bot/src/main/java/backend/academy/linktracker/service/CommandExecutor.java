package backend.academy.linktracker.service;

import backend.academy.linktracker.exception.ScrapperClientException;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandExecutor {

    public <T> T executeScrapperCall(Supplier<T> action, long chatId) {
        try {
            return action.get();
        } catch (ScrapperClientException e) {
            log.atError()
                    .setCause(e)
                    .addKeyValue("chatId", chatId)
                    .log("Ошибка при вызове Scrapper: {}", e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            log.atError()
                    .setCause(e)
                    .addKeyValue("chatId", chatId)
                    .log("Сетевая ошибка при вызове Scrapper: {}", e.getMessage());
            throw e;
        }
    }

    public void executeScrapperCallVoid(Runnable action, long chatId) {
        try {
            action.run();
        } catch (ScrapperClientException e) {
            log.atError()
                    .setCause(e)
                    .addKeyValue("chatId", chatId)
                    .log("Ошибка при вызове Scrapper: {}", e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            log.atError()
                    .setCause(e)
                    .addKeyValue("chatId", chatId)
                    .log("Сетевая ошибка при вызове Scrapper: {}", e.getMessage());
            throw e;
        }
    }
}
