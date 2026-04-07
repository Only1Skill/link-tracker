package backend.academy.linktracker.scrapper.repository.jpa.impl;

import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.ChatStorage;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import backend.academy.linktracker.scrapper.repository.TagStorage;
import backend.academy.linktracker.scrapper.repository.jpa.JpaChatRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaLinkRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaTagRepository;
import backend.academy.linktracker.scrapper.repository.jpa.entity.ChatEntity;
import backend.academy.linktracker.scrapper.repository.jpa.entity.LinkEntity;
import backend.academy.linktracker.scrapper.repository.jpa.entity.TagEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "ORM")
@Transactional
public class JpaLinkStorage implements LinkStorage {
    private final JpaLinkRepository linkRepository;
    private final JpaChatRepository chatRepository;
    private final JpaTagRepository tagRepository;
    private final TagStorage tagStorage;
    private final ChatStorage chatStorage;

    @Override
    public Link save(long chatId, Link link) {
        ChatEntity chat = chatRepository.findById(chatId).orElseGet(() -> {
            chatStorage.save(chatId);
            return chatRepository
                    .findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Ошибка при создании чата: " + chatId));
        });
        LinkEntity linkEntity = linkRepository.findByUrl(link.getUrl()).orElseGet(() -> {
            LinkEntity newEntity = LinkEntity.builder()
                    .url(link.getUrl())
                    .lastCheckTime(link.getLastCheckTime())
                    .lastUpdateTime(link.getLastUpdateTime())
                    .build();
            return linkRepository.save(newEntity);
        });
        if (link.getTags() != null && !link.getTags().isEmpty()) {
            for (String tagName : link.getTags()) {
                Long tagId = tagStorage.findOrCreate(tagName);
                TagEntity tag = tagRepository.getReferenceById(tagId);
                linkEntity.getTags().add(tag);
            }
        }
        if (!linkEntity.getChats().contains(chat)) {
            linkEntity.getChats().add(chat);
            chat.getLinks().add(linkEntity);
            linkRepository.save(linkEntity);
        }
        return toLink(linkEntity, chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findByChatId(long chatId) {
        Optional<ChatEntity> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) {
            return Collections.emptyList();
        }
        ChatEntity chat = chatOpt.orElseThrow(() -> new IllegalStateException("id чата не найдено"));
        return chat.getLinks().stream().map(entity -> toLink(entity, chatId)).collect(Collectors.toList());
    }

    @Override
    public List<Link> findByChatId(long chatId, String tag) {
        List<LinkEntity> entities = linkRepository.findByChatIdAndOptionalTag(chatId, tag);
        return entities.stream().map(entity -> toLink(entity, chatId)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Link> findByChatIdAndUrl(long chatId, String url) {
        return chatRepository.findById(chatId).flatMap(chat -> chat.getLinks().stream()
                .filter(link -> link.getUrl().equals(url))
                .findFirst()
                .map(entity -> toLink(entity, chatId)));
    }

    @Override
    public void delete(Long chatId, String url) {
        ChatEntity chat = chatRepository.findById(chatId).orElse(null);
        if (chat == null) return;
        linkRepository.findByUrl(url).ifPresent(link -> {
            if (link.getChats().contains(chat)) {
                link.getChats().remove(chat);
                chat.getLinks().remove(link);
                linkRepository.save(link);
                if (link.getChats().isEmpty()) {
                    linkRepository.delete(link);
                }
            }
        });
    }

    @Override
    public void deleteByChatId(Long chatId) {
        ChatEntity chat = chatRepository.findById(chatId).orElse(null);
        if (chat == null) return;
        for (LinkEntity link : new ArrayList<>(chat.getLinks())) {
            link.getChats().remove(chat);
            if (link.getChats().isEmpty()) {
                linkRepository.delete(link);
            } else {
                linkRepository.save(link);
            }
        }
        chat.getLinks().clear();
        chatRepository.save(chat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findAll() {
        List<Link> allSubscriptions = new ArrayList<>();
        for (ChatEntity chat : chatRepository.findAll()) {
            for (LinkEntity link : chat.getLinks()) {
                allSubscriptions.add(toLink(link, chat.getId()));
            }
        }
        return allSubscriptions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findAllByUrl(String url) {
        return linkRepository
                .findByUrl(url)
                .map(link -> link.getChats().stream()
                        .map(chat -> toLink(link, chat.getId()))
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    private Link toLink(LinkEntity entity, Long chatId) {
        List<String> tagNames =
                entity.getTags().stream().map(TagEntity::getName).collect(Collectors.toList());
        return Link.builder()
                .id(entity.getId())
                .chatId(chatId)
                .url(entity.getUrl())
                .tags(tagNames)
                .lastCheckTime(entity.getLastCheckTime())
                .lastUpdateTime(entity.getLastUpdateTime())
                .build();
    }
}
