package backend.academy.linktracker.scrapper.repository.jpa.impl;

import backend.academy.linktracker.scrapper.repository.ChatStorage;
import backend.academy.linktracker.scrapper.repository.jpa.JpaChatRepository;
import backend.academy.linktracker.scrapper.repository.jpa.entity.ChatEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@ConditionalOnProperty(name = "app.database.access-type", havingValue = "ORM")
@Repository
@RequiredArgsConstructor
public class JpaChatStorage implements ChatStorage {
    private final JpaChatRepository chatRepository;

    @Override
    public void save(Long chatId) {
        chatRepository.save(ChatEntity.builder().id(chatId).build());
    }

    @Override
    public boolean exists(Long chatId) {
        return chatRepository.existsById(chatId);
    }

    @Override
    public void delete(Long chatId) {
        chatRepository.deleteById(chatId);
    }
}
