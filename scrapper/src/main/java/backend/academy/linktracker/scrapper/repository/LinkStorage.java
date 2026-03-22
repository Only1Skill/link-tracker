package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.Link;
import java.util.List;
import java.util.Optional;

public interface LinkStorage {
    Link save(long chatId, Link link);

    List<Link> findByChatId(long chatId);

    Optional<Link> findByChatIdAndUrl(long chatId, String url);

    void delete(Long chatId, String url);

    void deleteByChatId(Long chatId);

    List<Link> findAll();

    List<Link> findAllByUrl(String url);
}
