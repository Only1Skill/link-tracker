package backend.academy.linktracker.scrapper.repository.inmemory;

import backend.academy.linktracker.scrapper.model.SubscriptionLinkView;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "app.database.access-type", havingValue = "IN_MEMORY")
@Component
public class InMemoryLinkStorage implements LinkStorage {

    private final Map<Long, List<SubscriptionLinkView>> storage = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public SubscriptionLinkView save(long chatId, SubscriptionLinkView link) {
        link.setId(nextId.getAndIncrement());
        storage.computeIfAbsent(chatId, k -> new ArrayList<>()).add(link);
        return link;
    }

    public List<SubscriptionLinkView> findByChatId(long chatId) {
        return storage.getOrDefault(chatId, new ArrayList<>());
    }

    public Optional<SubscriptionLinkView> findByChatIdAndUrl(long chatId, String url) {
        return storage.getOrDefault(chatId, new ArrayList<>()).stream()
                .filter(link -> link.getUrl().equals(url))
                .findFirst();
    }

    public void delete(Long chatId, String url) {
        storage.getOrDefault(chatId, new ArrayList<>())
                .removeIf(link -> link.getUrl().equals(url));
    }

    public void deleteByChatId(Long chatId) {
        storage.remove(chatId);
    }

    public List<SubscriptionLinkView> findAll() {
        return storage.values().stream().flatMap(List::stream).toList();
    }

    public List<SubscriptionLinkView> findAllByUrl(String url) {
        return storage.values().stream()
                .flatMap(List::stream)
                .filter(link -> link.getUrl().equals(url))
                .collect(Collectors.toList());
    }
}
