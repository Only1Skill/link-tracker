package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.time.OffsetDateTime;
import java.util.List;

public interface TrackedLinkStorage {
    List<TrackedLink> findNextBatchForCheck(int limit);

    List<Long> findSubscriberChatIds(Long linkId);

    void updateCheckTime(Long lindId, OffsetDateTime checkedAt);

    void updateLastUpdateTime(Long linkId, OffsetDateTime checkedAt);
}
