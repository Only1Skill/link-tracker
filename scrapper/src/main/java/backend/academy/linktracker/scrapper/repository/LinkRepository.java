package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.Link;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class LinkRepository {

    private final Map<Long, List<Link>> storage = new ConcurrentHashMap<>();
    private long nextId = 1;

    public Link save(long chatId, Link link) {
        link.setId(nextId++);
        storage.computeIfAbsent(chatId, k -> new ArrayList<>()).add(link);
        return link;
    }

    public List<Link> findByChatId(long chatId) {
        return storage.getOrDefault(chatId, new ArrayList<>());
    }

    public Optional<Link> findByChatIdAndUrl(long chatId, String url) {
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

    public List<Link> findAll() {
        return storage.values().stream().flatMap(List::stream).toList();
    }

    public List<Link> findAllByUrl(String url) {
        return storage.values().stream()
                .flatMap(List::stream)
                .filter(link -> link.getUrl().equals(url))
                .collect(Collectors.toList());
    }
}
