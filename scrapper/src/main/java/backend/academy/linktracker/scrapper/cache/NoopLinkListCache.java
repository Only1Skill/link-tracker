package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import java.util.List;
import java.util.Optional;

public class NoopLinkListCache implements LinkListCache {

    @Override
    public Optional<List<LinkResponse>> get(Long chatId) {
        return Optional.empty();
    }

    @Override
    public void put(Long chatId, List<LinkResponse> links) {}

    @Override
    public void evict(Long chatId) {}
}
