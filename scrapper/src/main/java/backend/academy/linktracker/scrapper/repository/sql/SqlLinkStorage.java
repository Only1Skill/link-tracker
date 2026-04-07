package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "SQL")
@SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
@Transactional
public class SqlLinkStorage implements LinkStorage {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Link> linkRowMapper = (rs, rowNum) -> Link.builder()
            .id(rs.getLong("id"))
            .chatId(rs.getLong("chat_id"))
            .url(rs.getString("url"))
            .tags(null)
            .lastCheckTime(rs.getTimestamp("last_check_time").toInstant().atOffset(ZoneOffset.UTC))
            .lastUpdateTime(rs.getTimestamp("last_update_time").toInstant().atOffset(ZoneOffset.UTC))
            .build();

    @Override
    public Link save(long chatId, Link link) {
        Optional<Long> existingLinkId = findLinkIdByUrl(link.getUrl());
        Long linkId;
        if (existingLinkId.isPresent()) {
            linkId = existingLinkId.orElseThrow(() -> new IllegalStateException("Id ссылки не найдено"));;
        } else {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                    connection -> {
                        PreparedStatement ps = connection.prepareStatement(
                                "INSERT INTO links (url, last_check_time, last_update_time) VALUES (?, ?, ?) RETURNING"
                                        + " id",
                                Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1, link.getUrl());
                        ps.setTimestamp(
                                2, Timestamp.from(link.getLastCheckTime().toInstant()));
                        ps.setTimestamp(
                                3, Timestamp.from(link.getLastUpdateTime().toInstant()));
                        return ps;
                    },
                    keyHolder);
            linkId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        }

        jdbcTemplate.update(
                "INSERT INTO link_chat (link_id, chat_id) VALUES (?, ?) ON CONFLICT DO NOTHING", linkId, chatId);

        if (link.getTags() != null && !link.getTags().isEmpty()) {
            List<Long> tagIds = new ArrayList<>();
            for (String tagName : link.getTags()) {
                Long tagId = findOrCreateTag(tagName);
                tagIds.add(tagId);
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO link_tags (link_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    tagIds,
                    tagIds.size(),
                    (ps, tagId) -> {
                        ps.setLong(1, linkId);
                        ps.setLong(2, tagId);
                    });
        }
        return Link.builder()
                .id(linkId)
                .chatId(chatId)
                .url(link.getUrl())
                .tags(link.getTags())
                .lastCheckTime(link.getLastCheckTime())
                .lastUpdateTime(link.getLastUpdateTime())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findByChatId(long chatId) {
        List<Link> links = jdbcTemplate.query(
                "SELECT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id "
                        + "FROM links l JOIN link_chat lc ON l.id = lc.link_id "
                        + "WHERE lc.chat_id = ?",
                linkRowMapper,
                chatId);
        for (Link link : links) {
            List<String> tags = jdbcTemplate.queryForList(
                    "SELECT t.name FROM tags t JOIN link_tags lt ON t.id = lt.tag_id WHERE lt.link_id = ?",
                    String.class,
                    link.getId());
            link.setTags(tags);
        }
        return links;
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<Link> findByChatId(long chatId, String tag) {
        String linksSql;
        Object[] params;
        if (tag == null || tag.isBlank()) {
            linksSql = """
                SELECT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id
                FROM links l
                JOIN link_chat lc ON l.id = lc.link_id
                WHERE lc.chat_id = ?
                """;
            params = new Object[] {chatId};
        } else {
            linksSql = """
                SELECT DISTINCT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id
                FROM links l
                JOIN link_chat lc ON l.id = lc.link_id
                JOIN link_tags lt ON l.id = lt.link_id
                JOIN tags t ON lt.tag_id = t.id
                WHERE lc.chat_id = ? AND t.name = ?
                """;
            params = new Object[] {chatId, tag};
        }
        List<Link> links = jdbcTemplate.query(linksSql, linkRowMapper, params);
        if (links.isEmpty()) {
            return links;
        }
        String tagSql = """
            SELECT lt.link_id, t.name
            FROM link_tags lt
            JOIN tags t ON lt.tag_id = t.id
            WHERE lt.link_id IN (
            """ + String.join(",", Collections.nCopies(links.size(), "?")) + ")";
        List<Long> linkIds = links.stream().map(Link::getId).toList();
        Map<Long, List<String>> tagsByLinkId = new HashMap<>();
        jdbcTemplate.query(tagSql, linkIds.toArray(), rs -> {
            tagsByLinkId
                    .computeIfAbsent(rs.getLong("link_id"), k -> new ArrayList<>())
                    .add(rs.getString("name"));
        });
        for (Link link : links) {
            link.setTags(tagsByLinkId.getOrDefault(link.getId(), List.of()));
        }
        return links;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Link> findByChatIdAndUrl(long chatId, String url) {
        List<Link> links = jdbcTemplate.query(
                "SELECT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id "
                        + "FROM links l JOIN link_chat lc ON l.id = lc.link_id "
                        + "WHERE lc.chat_id = ? AND l.url = ?",
                linkRowMapper,
                chatId,
                url);
        if (links.isEmpty()) {
            return Optional.empty();
        }
        Link link = links.getFirst();
        List<String> tags = jdbcTemplate.queryForList(
                "SELECT t.name FROM tags t JOIN link_tags lt ON t.id = lt.tag_id WHERE lt.link_id = ?",
                String.class,
                link.getId());
        link.setTags(tags);
        return Optional.of(link);
    }

    @Override
    public void delete(Long chatId, String url) {
        Optional<Long> linkIdOpt = findLinkIdByUrl(url);
        if (linkIdOpt.isEmpty()) {
            return;
        }
        Long linkId = linkIdOpt.orElseThrow(() -> new IllegalStateException("Id ссылки не найдено"));
        String sql = """
            WITH deleted_chat AS (
                DELETE FROM link_chat WHERE chat_id = ? AND link_id = ? RETURNING link_id
            ),
            no_other_chats AS (
                SELECT NOT EXISTS (SELECT 1 FROM link_chat WHERE link_id = (SELECT link_id FROM deleted_chat)) AS is_last
            ),
            deleted_link AS (
                DELETE FROM links
                WHERE id = (SELECT link_id FROM deleted_chat)
                AND (SELECT is_last FROM no_other_chats) = true
                RETURNING id
            )
            DELETE FROM link_tags
            WHERE link_id = (SELECT id FROM deleted_link)
            """;
        jdbcTemplate.update(sql, chatId, linkId);
    }

    @Override
    public void deleteByChatId(Long chatId) {
        List<Long> linkIds =
                jdbcTemplate.queryForList("SELECT link_id FROM link_chat WHERE chat_id = ?", Long.class, chatId);
        jdbcTemplate.update("DELETE FROM link_chat WHERE chat_id = ?", chatId);
        for (Long linkId : linkIds) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM link_chat WHERE link_id = ?", Integer.class, linkId);
            if (count == 0) {
                jdbcTemplate.update("DELETE FROM link_tags WHERE link_id = ?", linkId);
                jdbcTemplate.update("DELETE FROM links WHERE id = ?", linkId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findAll() {
        String sql = """
            SELECT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id,
                   COALESCE(STRING_AGG(t.name, ','), '') AS tags
            FROM links l
            JOIN link_chat lc ON l.id = lc.link_id
            LEFT JOIN link_tags lt ON l.id = lt.link_id
            LEFT JOIN tags t ON lt.tag_id = t.id
            GROUP BY l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String tagsStr = rs.getString("tags");
            List<String> tags = tagsStr.isEmpty() ? List.of() : Arrays.asList(tagsStr.split(","));
            return Link.builder()
                    .id(rs.getLong("id"))
                    .chatId(rs.getLong("chat_id"))
                    .url(rs.getString("url"))
                    .lastCheckTime(
                            rs.getTimestamp("last_check_time").toInstant().atOffset(ZoneOffset.UTC))
                    .lastUpdateTime(
                            rs.getTimestamp("last_update_time").toInstant().atOffset(ZoneOffset.UTC))
                    .tags(tags)
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findAllByUrl(String url) {
        List<Link> links = jdbcTemplate.query(
                "SELECT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id "
                        + "FROM links l JOIN link_chat lc ON l.id = lc.link_id WHERE l.url = ?",
                (rs, rowNum) -> Link.builder()
                        .id(rs.getLong("id"))
                        .chatId(rs.getLong("chat_id"))
                        .url(rs.getString("url"))
                        .lastCheckTime(
                                rs.getTimestamp("last_check_time").toInstant().atOffset(ZoneOffset.UTC))
                        .lastUpdateTime(
                                rs.getTimestamp("last_update_time").toInstant().atOffset(ZoneOffset.UTC))
                        .build(),
                url);
        for (Link link : links) {
            List<String> tags = jdbcTemplate.queryForList(
                    "SELECT t.name FROM tags t JOIN link_tags lt ON t.id = lt.tag_id WHERE lt.link_id = ?",
                    String.class,
                    link.getId());
            link.setTags(tags);
        }
        return links;
    }

    private Optional<Long> findLinkIdByUrl(String url) {
        List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM links WHERE url = ?", Long.class, url);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.getFirst());
    }

    private Long findOrCreateTag(String name) {
        List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM tags WHERE name = ?", Long.class, name);
        if (!ids.isEmpty()) {
            return ids.getFirst();
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO tags (name) VALUES (?) RETURNING id", Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, name);
                    return ps;
                },
                keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}
