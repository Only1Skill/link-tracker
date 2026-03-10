package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.internal.annotation.SuppressFBWarnings;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressFBWarnings("CRLF_INJECTION_LOGS")
public class ChatService {

    private final ChatRepository chatRepository;
    private final LinkRepository linkRepository;

    /**
     * Регистрирует новый чат.
     *
     * @param chatId идентификатор чата
     * @throws ChatAlreadyExistsException если чат уже зарегистрирован
     */
    public void register(Long chatId) {
        if (chatRepository.exists(chatId)) {
            throw new ChatAlreadyExistsException(chatId);
        }
        chatRepository.save(chatId);
        log.info("Chat registered: {}", chatId);
    }

    /**
     * Удаляет чат и все его ссылки.
     *
     * @param chatId идентификатор чата
     * @throws ChatNotFoundException если чат не найден
     */
    public void delete(Long chatId) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        linkRepository.deleteByChatId(chatId);
        chatRepository.delete(chatId);
        log.info("Chat deleted: {}", chatId);
    }
}
