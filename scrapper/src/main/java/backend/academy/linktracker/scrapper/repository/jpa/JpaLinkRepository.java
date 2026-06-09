package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.repository.jpa.entity.LinkEntity;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaLinkRepository extends JpaRepository<LinkEntity, Long> {

    Optional<LinkEntity> findByUrl(String url);

    @Query("""
        select l
        from LinkEntity l
        where l.lastCheckTime < :checkBefore
        order by l.lastCheckTime asc
        """)
    java.util.List<LinkEntity> findNextBatchForCheck(
            @Param("checkBefore") OffsetDateTime checkBefore, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update LinkEntity l
        set l.lastCheckTime = :checkedAt,
            l.lastUpdateTime = :lastUpdatedAt
        where l.id = :linkId
        """)
    void updateProcessingState(
            @Param("linkId") Long linkId,
            @Param("checkedAt") OffsetDateTime checkedAt,
            @Param("lastUpdatedAt") OffsetDateTime lastUpdatedAt);
}
