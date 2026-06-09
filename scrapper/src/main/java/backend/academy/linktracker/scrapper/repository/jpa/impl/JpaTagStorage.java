package backend.academy.linktracker.scrapper.repository.jpa.impl;

import backend.academy.linktracker.scrapper.repository.TagStorage;
import backend.academy.linktracker.scrapper.repository.jpa.JpaTagRepository;
import backend.academy.linktracker.scrapper.repository.jpa.entity.TagEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "ORM")
@RequiredArgsConstructor
@Transactional
public class JpaTagStorage implements TagStorage {
    private final JpaTagRepository tagRepository;

    @Override
    public Long findOrCreate(String name) {
        return tagRepository.findByName(name).map(TagEntity::getId).orElseGet(() -> {
            TagEntity newTag = TagEntity.builder().name(name).build();
            return tagRepository.save(newTag).getId();
        });
    }
}
