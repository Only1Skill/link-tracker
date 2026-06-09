package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.SubscriptionLinkView;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface LinkStorage {
    SubscriptionLinkView save(long chatId, SubscriptionLinkView link);

    List<SubscriptionLinkView> findByChatId(long chatId);

    default List<SubscriptionLinkView> findByChatId(long chatId, String tag) {
        List<SubscriptionLinkView> links = findByChatId(chatId);
        if (tag == null || tag.isBlank()) {
            return links;
        }
        return links.stream()
                .filter(link -> link.getTags() != null && link.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    Optional<SubscriptionLinkView> findByChatIdAndUrl(long chatId, String url);

    void delete(Long chatId, String url);

    void deleteByChatId(Long chatId);

    List<SubscriptionLinkView> findAll();

    List<SubscriptionLinkView> findAllByUrl(String url);
}
