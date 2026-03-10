package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkDuplicateException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.util.LogSanitizer;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final ChatRepository chatRepository;

    /**
     * Добавить ссылку для указанного чата.
     *
     * @param chatId  идентификатор чата
     * @param request данные ссылки (url и теги)
     * @return LinkResponse созданной ссылки
     * @throws ChatNotFoundException  если чат не зарегистрирован
     * @throws LinkDuplicateException если ссылка уже отслеживается в этом чате
     */
    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    public LinkResponse addLink(Long chatId, AddLinkRequest request) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        if (linkRepository.findByChatIdAndUrl(chatId, request.getLink()).isPresent()) {
            throw new LinkDuplicateException(request.getLink());
        }

        Link link = Link.builder()
                .chatId(chatId)
                .url(request.getLink())
                .tags(request.getTags())
                .lastCheckTime(OffsetDateTime.now())
                .lastUpdateTime(OffsetDateTime.now())
                .build();

        Link saved = linkRepository.save(chatId, link);
        log.info("Link added: {} for chat {}", LogSanitizer.sanitize(LogSanitizer.sanitize(saved.getUrl())), chatId);

        return mapToResponse(saved);
    }

    /**
     * Получить все ссылки чата, опционально фильтруя по тегу.
     *
     * @param chatId идентификатор чата
     * @param tag    тег для фильтрации (может быть null)
     * @return список LinkResponse
     * @throws ChatNotFoundException если чат не зарегистрирован
     */
    public List<LinkResponse> getLinks(Long chatId, String tag) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

        List<Link> links = linkRepository.findByChatId(chatId);
        if (tag != null && !tag.isBlank()) {
            links = links.stream()
                    .filter(link -> link.getTags() != null && link.getTags().contains(tag))
                    .toList();
        }

        return links.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Удалить ссылку из отслеживаемых.
     *
     * @param chatId идентификатор чата
     * @param url    ссылка для удаления
     * @throws ChatNotFoundException если чат не зарегистрирован
     * @throws LinkNotFoundException если ссылка не найдена
     */
    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    public void removeLink(Long chatId, String url) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

        Link link = linkRepository.findByChatIdAndUrl(chatId, url).orElseThrow(() -> new LinkNotFoundException(url));

        linkRepository.delete(chatId, url);
        log.info("Removing link: {} for chat {}", LogSanitizer.sanitize(LogSanitizer.sanitize(link.getUrl())), chatId);
    }

    private LinkResponse mapToResponse(Link link) {
        return LinkResponse.builder()
                .id(link.getId())
                .url(URI.create(link.getUrl()))
                .tags(link.getTags())
                .lastUpdate(link.getLastUpdateTime())
                .build();
    }
}
