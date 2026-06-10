package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.cache.LinkListCache;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkApplicationService {
    private final LinkService linkService;
    private final LinkListCache linkListCache;

    public LinkResponse addLink(Long chatId, AddLinkRequest request) {
        LinkResponse response = linkService.addLink(chatId, request);

        linkListCache.evict(chatId);
        log.debug("Кэш списка ссылок очищен после добавления ссылки");

        return response;
    }

    public List<LinkResponse> getLinks(Long chatId, String tag) {
        if (tag != null && !tag.isBlank()) {
            log.debug("Кэш списка ссылок не используется, так как передан фильтр по тегу");
            return linkService.getLinks(chatId, tag);
        }

        return linkListCache
                .get(chatId)
                .map(links -> {
                    log.debug("Список ссылок получен из кэша");
                    return links;
                })
                .orElseGet(() -> {
                    log.debug("Список ссылок отсутствует в кэше, загрузка из хранилища");
                    return loadAndCacheLinks(chatId);
                });
    }

    public void removeLink(Long chatId, String url) {
        linkService.removeLink(chatId, url);

        linkListCache.evict(chatId);
        log.debug("Кэш списка ссылок очищен после удаления ссылки");
    }

    private List<LinkResponse> loadAndCacheLinks(Long chatId) {
        List<LinkResponse> links = linkService.getLinks(chatId, null);

        linkListCache.put(chatId, links);
        log.debug("Список ссылок загружен из хранилища и сохранён в кэш");

        return links;
    }
}
