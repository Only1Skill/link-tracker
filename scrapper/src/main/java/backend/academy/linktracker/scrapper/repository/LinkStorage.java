package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.Link;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface LinkStorage {
    Link save(long chatId, Link link);

    List<Link> findByChatId(long chatId);

    default List<Link> findByChatId(long chatId, String tag) {
        List<Link> links = findByChatId(chatId);
        if (tag == null || tag.isBlank()) {
            return links;
        }
        return links.stream()
                .filter(link -> link.getTags() != null && link.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    Optional<Link> findByChatIdAndUrl(long chatId, String url);

    void delete(Long chatId, String url);

    void deleteByChatId(Long chatId);

    List<Link> findAll();

    List<Link> findAllByUrl(String url);
}
