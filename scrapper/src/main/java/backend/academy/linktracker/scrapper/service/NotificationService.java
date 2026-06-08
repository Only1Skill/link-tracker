package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.LinkUpdateBatch;
import backend.academy.linktracker.scrapper.model.LinkEvent;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.service.formatter.UpdateMessageFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final BotClient botClient;
    private final UpdateMessageFormatter updateMessageFormatter;

    public void sendUpdates(TrackedLink trackedLink, List<Long> chatIds, List<LinkEvent> events) {
        if (trackedLink == null) {
            throw new IllegalArgumentException("trackedLink не должна быть пустой");
        }

        if (chatIds == null || chatIds.isEmpty()) {
            log.debug("Нет подписчиков для ссылки id={}, url={}", trackedLink.getId(), trackedLink.getUrl());
            return;
        }

        if (events == null || events.isEmpty()) {
            log.debug("Нет обновлений для ссылки id={}, url={}", trackedLink.getId(), trackedLink.getUrl());
            return;
        }

        List<Long> normalizedChatIds =
                chatIds.stream().filter(id -> id != null).distinct().toList();

        if (normalizedChatIds.isEmpty()) {
            log.debug("Не валидный подписчик для ссылки id={}, url={}", trackedLink.getId(), trackedLink.getUrl());
            return;
        }

        List<LinkUpdate> updates = events.stream()
                .map(event -> new LinkUpdate(
                        trackedLink.getId(),
                        trackedLink.getUrl(),
                        updateMessageFormatter.format(event),
                        normalizedChatIds))
                .toList();

        botClient.sendUpdates(new LinkUpdateBatch(updates));

        log.debug(
                "Batch уведомлений отправлен для ссылки id={}, events={}, subscribers={}",
                trackedLink.getId(),
                updates.size(),
                normalizedChatIds.size());
    }
}
