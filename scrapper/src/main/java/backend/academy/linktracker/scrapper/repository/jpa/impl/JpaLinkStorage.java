package backend.academy.linktracker.scrapper.repository.jpa.impl;

import backend.academy.linktracker.scrapper.model.SubscriptionLinkView;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import backend.academy.linktracker.scrapper.repository.jpa.JpaChatRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaLinkRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaTagRepository;
import backend.academy.linktracker.scrapper.repository.jpa.entity.ChatEntity;
import backend.academy.linktracker.scrapper.repository.jpa.entity.LinkEntity;
import backend.academy.linktracker.scrapper.repository.jpa.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.jpa.entity.SubscriptionId;
import backend.academy.linktracker.scrapper.repository.jpa.entity.TagEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    private final JpaChatRepository chatRepository;
    private final JpaLinkRepository linkRepository;
    private final JpaTagRepository tagRepository;
    private final JpaSubscriptionRepository subscriptionRepository;

    @Override
    public SubscriptionLinkView save(long chatId, SubscriptionLinkView link) {
        ChatEntity chat = chatRepository
                .findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Чат не найден: " + chatId));

        LinkEntity linkEntity = linkRepository
                .findByUrl(link.getUrl())
                .orElseGet(() -> linkRepository.save(LinkEntity.builder()
                        .url(link.getUrl())
                        .lastCheckTime(safeTime(link.getLastCheckTime()))
                        .lastUpdateTime(safeTime(link.getLastUpdateTime()))
                        .build()));

        SubscriptionId subscriptionId = new SubscriptionId(chatId, linkEntity.getId());

        SubscriptionEntity subscription = subscriptionRepository
                .findById(subscriptionId)
                .orElseGet(() -> subscriptionRepository.save(SubscriptionEntity.builder()
                        .id(subscriptionId)
                        .chat(chat)
                        .link(linkEntity)
                        .build()));

        List<String> normalizedTags = normalizeTags(link.getTags());
        for (String tagName : normalizedTags) {
            TagEntity tag = tagRepository
                    .findByName(tagName)
                    .orElseGet(() ->
                            tagRepository.save(TagEntity.builder().name(tagName).build()));
            subscription.addTag(tag);
        }

        SubscriptionEntity saved = subscriptionRepository.save(subscription);
        return toView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionLinkView> findByChatId(long chatId) {
        return subscriptionRepository.findByChat_Id(chatId).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionLinkView> findByChatId(long chatId, String tag) {
        if (tag == null || tag.isBlank()) {
            return findByChatId(chatId);
        }

        return subscriptionRepository.findByChatIdAndTag(chatId, tag).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionLinkView> findByChatIdAndUrl(long chatId, String url) {
        return subscriptionRepository.findByChat_IdAndLink_Url(chatId, url).map(this::toView);
    }

    @Override
    public void delete(Long chatId, String url) {
        Optional<SubscriptionEntity> subscriptionOpt = subscriptionRepository.findByChat_IdAndLink_Url(chatId, url);
        if (subscriptionOpt.isEmpty()) {
            return;
        }

        SubscriptionEntity subscription =
                subscriptionOpt.orElseThrow(() -> new IllegalArgumentException("Подписка не найдена"));
        Long linkId = subscription.getLink().getId();

        subscriptionRepository.delete(subscription);
        subscriptionRepository.flush();

        if (!subscriptionRepository.existsByLink_Id(linkId)) {
            linkRepository.deleteById(linkId);
        }
    }

    @Override
    public void deleteByChatId(Long chatId) {
        List<SubscriptionEntity> subscriptions = subscriptionRepository.findByChat_Id(chatId);
        List<Long> linkIds = subscriptions.stream()
                .map(subscription -> subscription.getLink().getId())
                .distinct()
                .toList();

        subscriptionRepository.deleteAll(subscriptions);
        subscriptionRepository.flush();

        for (Long linkId : linkIds) {
            if (!subscriptionRepository.existsByLink_Id(linkId)) {
                linkRepository.deleteById(linkId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionLinkView> findAll() {
        return subscriptionRepository.findAllWithGraph().stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionLinkView> findAllByUrl(String url) {
        return subscriptionRepository.findByLink_Url(url).stream()
                .map(this::toView)
                .toList();
    }

    private SubscriptionLinkView toView(SubscriptionEntity subscription) {
        List<String> tags = subscription.getSubscriptionTags().stream()
                .map(subscriptionTag -> subscriptionTag.getTag().getName())
                .distinct()
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        return SubscriptionLinkView.builder()
                .id(subscription.getLink().getId())
                .chatId(subscription.getChat().getId())
                .url(subscription.getLink().getUrl())
                .lastCheckTime(subscription.getLink().getLastCheckTime())
                .lastUpdateTime(subscription.getLink().getLastUpdateTime())
                .tags(tags)
                .build();
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        return tags.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private OffsetDateTime safeTime(OffsetDateTime value) {
        return value != null ? value : OffsetDateTime.now(ZoneOffset.UTC);
    }
}
