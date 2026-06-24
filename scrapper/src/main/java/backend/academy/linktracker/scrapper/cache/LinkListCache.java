package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import java.util.List;
import java.util.Optional;

public interface LinkListCache {

    Optional<List<LinkResponse>> get(Long chatId);

    void put(Long chatId, List<LinkResponse> links);

    void evict(Long chatId);
}
