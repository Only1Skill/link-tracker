package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.repository.ChatStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@ConditionalOnProperty(name = "app.database.access-type", havingValue = "SQL")
@Repository
@RequiredArgsConstructor
public class SqlChatStorage implements ChatStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(Long chatId) {
        jdbcTemplate.update("INSERT INTO chats (id) VALUES (?)", chatId);
    }

    @Override
    public boolean exists(Long chatId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chats WHERE id = ?", Integer.class, chatId);
        return count != null && count > 0;
    }

    @Override
    public void delete(Long chatId) {
        jdbcTemplate.update("DELETE FROM chats WHERE id = ?", chatId);
    }
}
