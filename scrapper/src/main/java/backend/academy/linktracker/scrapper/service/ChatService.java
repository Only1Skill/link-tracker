package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.repository.ChatStorage;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.internal.annotation.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressFBWarnings("CRLF_INJECTION_LOGS")
@Transactional
public class ChatService {

    private final ChatStorage chatStorage;
    private final LinkStorage linkStorage;

    /**
     * Регистрирует новый чат.
     *
     * @param chatId идентификатор чата
     * @throws ChatAlreadyExistsException если чат уже зарегистрирован
     */
    public void register(Long chatId) {
        if (chatStorage.exists(chatId)) {
            throw new ChatAlreadyExistsException(chatId);
        }
        chatStorage.save(chatId);
        log.info("Chat registered: {}", chatId);
    }

    /**
     * Удаляет чат и все его ссылки.
     *
     * @param chatId идентификатор чата
     * @throws ChatNotFoundException если чат не найден
     */
    public void delete(Long chatId) {
        if (!chatStorage.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        linkStorage.deleteByChatId(chatId);
        chatStorage.delete(chatId);
        log.info("Chat deleted: {}", chatId);
    }
}
