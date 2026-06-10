package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.cache.LinkListCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatApplicationService {
    private final ChatService chatService;
    private final LinkListCache linkListCache;

    public void register(Long chatId) {
        chatService.register(chatId);
    }

    public void delete(Long chatId) {
        chatService.delete(chatId);

        linkListCache.evict(chatId);
        log.debug("Кэш списка ссылок очищен после удаления чата, chatId={}", chatId);
    }
}
