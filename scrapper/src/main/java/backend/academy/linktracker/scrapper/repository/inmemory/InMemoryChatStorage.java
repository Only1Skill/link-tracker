package backend.academy.linktracker.scrapper.repository.inmemory;

import backend.academy.linktracker.scrapper.repository.ChatStorage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "app.database.access-type", havingValue = "IN_MEMORY")
@Component
public class InMemoryChatStorage implements ChatStorage {
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
