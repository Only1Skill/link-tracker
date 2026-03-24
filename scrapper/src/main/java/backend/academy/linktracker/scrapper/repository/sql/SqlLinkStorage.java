package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "SQL")
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

    @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
    @Override
    public Link save(long chatId, Link link) {
        Optional<Long> existingLinkId = findLinkIdByUrl(link.getUrl());
        Long linkId;
        if (existingLinkId.isPresent()) {
            linkId = existingLinkId.orElseThrow(
                    () -> new IllegalStateException("Идентификатор ссылки должен существовать"));
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
            for (String tagName : link.getTags()) {
                Long tagId = findOrCreateTag(tagName);
                jdbcTemplate.update(
                        "INSERT INTO link_tags (link_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING", linkId, tagId);
            }
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
        Optional<Long> linkId = findLinkIdByUrl(url);
        if (linkId.isEmpty()) {
            return;
        }
        jdbcTemplate.update("DELETE FROM link_chat WHERE chat_id = ? AND link_id = ?", chatId, linkId.orElseThrow());
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM link_chat WHERE link_id = ?", Integer.class, linkId.orElseThrow());
        if (count == 0) {
            jdbcTemplate.update("DELETE FROM link_tags WHERE link_id = ?", linkId.orElseThrow());
            jdbcTemplate.update("DELETE FROM links WHERE id = ?", linkId.orElseThrow());
        }
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
    public List<Link> findAll() {
        List<Link> allLinks = jdbcTemplate.query(
                "SELECT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id "
                        + "FROM links l JOIN link_chat lc ON l.id = lc.link_id",
                (rs, rowNum) -> Link.builder()
                        .id(rs.getLong("id"))
                        .chatId(rs.getLong("chat_id"))
                        .url(rs.getString("url"))
                        .lastCheckTime(
                                rs.getTimestamp("last_check_time").toInstant().atOffset(ZoneOffset.UTC))
                        .lastUpdateTime(
                                rs.getTimestamp("last_update_time").toInstant().atOffset(ZoneOffset.UTC))
                        .build());
        for (Link link : allLinks) {
            List<String> tags = jdbcTemplate.queryForList(
                    "SELECT t.name FROM tags t JOIN link_tags lt ON t.id = lt.tag_id WHERE lt.link_id = ?",
                    String.class,
                    link.getId());
            link.setTags(tags);
        }
        return allLinks;
    }

    @Override
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
