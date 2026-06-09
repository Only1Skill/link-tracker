package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.TrackedLinkStorage;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "SQL")
@Transactional
public class SqlTrackedLinkStorage implements TrackedLinkStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<TrackedLink> findNextBatchForCheck(int limit, OffsetDateTime checkBefore) {
        String sql = """
            select id, url, last_check_time, last_update_time
            from links
            where last_check_time is null or last_check_time < ?
            order by last_check_time asc nulls first
            limit ?
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> TrackedLink.builder()
                        .id(rs.getLong("id"))
                        .url(rs.getString("url"))
                        .lastCheckTime(rs.getObject("last_check_time", OffsetDateTime.class))
                        .lastUpdateTime(rs.getObject("last_update_time", OffsetDateTime.class))
                        .build(),
                Timestamp.from(checkBefore.toInstant()),
                limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findSubscriberChatIds(Long linkId) {
        return jdbcTemplate.queryForList("select chat_id from link_chat where link_id = ?", Long.class, linkId);
    }

    @Override
    public void updateProcessingState(Long linkId, OffsetDateTime checkedAt, OffsetDateTime lastUpdatedAt) {
        jdbcTemplate.update(
                """
                update links
                set last_check_time = ?,
                    last_update_time = ?
                where id = ?
                """, Timestamp.from(checkedAt.toInstant()), Timestamp.from(lastUpdatedAt.toInstant()), linkId);
    }
}
