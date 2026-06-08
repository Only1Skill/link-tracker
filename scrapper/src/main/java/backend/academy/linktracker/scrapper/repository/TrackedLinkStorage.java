package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.time.OffsetDateTime;
import java.util.List;

public interface TrackedLinkStorage {

    List<TrackedLink> findNextBatchForCheck(int limit, OffsetDateTime checkBefore);

    List<Long> findSubscriberChatIds(Long linkId);

    void updateProcessingState(Long linkId, OffsetDateTime checkedAt, OffsetDateTime lastUpdatedAt);
}
