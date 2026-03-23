package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.repository.jpa.entity.LinkEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaLinkRepository extends JpaRepository<LinkEntity, Long> {
    Optional<LinkEntity> findByUrl(String url);
}
