package backend.academy.linktracker.scrapper.repository.jpa.impl;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.TrackedLinkStorage;
import backend.academy.linktracker.scrapper.repository.jpa.JpaLinkRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.jpa.entity.LinkEntity;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "ORM")
@Transactional
public class JpaTrackedLinkStorage implements TrackedLinkStorage {

    private final JpaLinkRepository linkRepository;
    private final JpaSubscriptionRepository subscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TrackedLink> findNextBatchForCheck(int limit) {
        return linkRepository.findNextBatchForCheck(PageRequest.of(0, limit)).stream()
                .map(this::toTrackedLink)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findSubscriberChatIds(Long linkId) {
        return subscriptionRepository.findSubscriberChatIdsByLinkId(linkId);
    }

    @Override
    public void updateCheckTime(Long linkId, OffsetDateTime checkedAt) {
        linkRepository.updateCheckTime(linkId, checkedAt);
    }

    @Override
    public void updateLastUpdateTime(Long linkId, OffsetDateTime checkedAt) {
        linkRepository.updateLastUpdateTime(linkId, checkedAt);
    }

    private TrackedLink toTrackedLink(LinkEntity entity) {
        return TrackedLink.builder()
                .id(entity.getId())
                .url(entity.getUrl())
                .lastCheckTime(entity.getLastCheckTime())
                .lastUpdateTime(entity.getLastUpdateTime())
                .build();
    }
}
