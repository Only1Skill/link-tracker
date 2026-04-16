package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.repository.jpa.entity.LinkEntity;
import java.time.OffsetDateTime;
import java.util.List;
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
        order by l.lastCheckTime asc
        """)
    List<LinkEntity> findNextBatchForCheck(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update LinkEntity l
           set l.lastCheckTime = :checkedAt
           where l.id = :linkId
           """)
    void updateCheckTime(@Param("linkId") Long linkId, @Param("checkedAt") OffsetDateTime checkedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update LinkEntity l
           set l.lastUpdateTime = :checkedAt
           where l.id = :linkId
           """)
    void updateLastUpdateTime(@Param("linkId") Long linkId, @Param("checkedAt") OffsetDateTime checkedAt);
}
