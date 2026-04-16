package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkDuplicateException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.model.SubscriptionLinkView;
import backend.academy.linktracker.scrapper.repository.ChatStorage;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import backend.academy.linktracker.scrapper.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LinkService {
    private final LinkStorage linkStorage;
    private final ChatStorage chatStorage;

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
        if (!chatStorage.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        if (linkStorage.findByChatIdAndUrl(chatId, request.link()).isPresent()) {
            throw new LinkDuplicateException(request.link());
        }

        SubscriptionLinkView link = SubscriptionLinkView.builder()
                .chatId(chatId)
                .url(request.link())
                .tags(request.tags())
                .lastCheckTime(OffsetDateTime.now(ZoneOffset.UTC))
                .lastUpdateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        SubscriptionLinkView saved = linkStorage.save(chatId, link);

        List<String> safeTags = saved.getTags() == null
                ? null
                : saved.getTags().stream().map(LogSanitizer::sanitize).collect(Collectors.toList());
        log.info("Link added: {} for chat {}, tags={}", LogSanitizer.sanitize(saved.getUrl()), chatId, safeTags);

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
    @Transactional(readOnly = true)
    public List<LinkResponse> getLinks(Long chatId, String tag) {
        if (!chatStorage.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

        List<SubscriptionLinkView> links = linkStorage.findByChatId(chatId, tag);
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
    @Transactional
    public void removeLink(Long chatId, String url) {
        if (!chatStorage.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

        SubscriptionLinkView link =
                linkStorage.findByChatIdAndUrl(chatId, url).orElseThrow(() -> new LinkNotFoundException(url));

        linkStorage.delete(chatId, url);
        log.info("Removing link: {} for chat {}", LogSanitizer.sanitize(link.getUrl()), chatId);
    }

    private LinkResponse mapToResponse(SubscriptionLinkView link) {
        return new LinkResponse(link.getId(), URI.create(link.getUrl()), link.getTags(), link.getLastUpdateTime());
    }
}
