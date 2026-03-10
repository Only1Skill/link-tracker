package backend.academy.linktracker.scrapper.repository;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRepository {
    private final Set<Long> chats = ConcurrentHashMap.newKeySet();

    public void save(Long chatId) {
        chats.add(chatId);
    }

    public boolean exists(Long chatId) {
        return chats.contains(chatId);
    }

    public void delete(Long chatId) {
        chats.remove(chatId);
    }
}
