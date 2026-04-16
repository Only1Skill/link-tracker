package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.TrackedLinkStorage;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "SQL")
@Transactional
public class SqlTrackedLinkStorage implements TrackedLinkStorage {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TrackedLink> trackedLinkRowMapper = (rs, rowNum) -> TrackedLink.builder()
            .id(rs.getLong("id"))
            .url(rs.getString("url"))
            .lastCheckTime(rs.getObject("last_check_time", OffsetDateTime.class))
            .lastUpdateTime(rs.getObject("last_update_time", OffsetDateTime.class))
            .build();

    @Override
    @Transactional(readOnly = true)
    public List<TrackedLink> findNextBatchForCheck(int limit) {
        return jdbcTemplate.query("""
            SELECT id, url, last_check_time, last_update_time
            FROM links
            ORDER BY last_check_time ASC, id ASC
            LIMIT ?
            """, trackedLinkRowMapper, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findSubscriberChatIds(Long linkId) {
        return jdbcTemplate.queryForList("""
            SELECT chat_id
            FROM link_chat
            WHERE link_id = ?
            ORDER BY chat_id
            """, Long.class, linkId);
    }

    @Override
    public void updateCheckTime(Long linkId, OffsetDateTime checkedAt) {
        jdbcTemplate.update("""
            UPDATE links
            SET last_check_time = ?
            WHERE id = ?
            """, Timestamp.from(checkedAt.toInstant()), linkId);
    }

    @Override
    public void updateLastUpdateTime(Long linkId, OffsetDateTime checkedAt) {
        jdbcTemplate.update("""
            UPDATE links
            SET last_update_time = ?
            WHERE id = ?
            """, Timestamp.from(checkedAt.toInstant()), linkId);
    }
}
