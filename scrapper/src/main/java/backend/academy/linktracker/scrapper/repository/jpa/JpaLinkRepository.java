package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.repository.jpa.entity.LinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaLinkRepository extends JpaRepository<LinkEntity, Long> {
    Optional<LinkEntity> findByUrl(String url);

    @Query("""
        SELECT DISTINCT l
        FROM LinkEntity l
        JOIN FETCH l.tags t
        WHERE EXISTS (SELECT c FROM l.chats c WHERE c.id = :chatId)
          AND (:tag IS NULL OR t.name = :tag)
        """)
    List<LinkEntity> findByChatIdAndOptionalTag(@Param("chatId") Long chatId, @Param("tag") String tag);
}
