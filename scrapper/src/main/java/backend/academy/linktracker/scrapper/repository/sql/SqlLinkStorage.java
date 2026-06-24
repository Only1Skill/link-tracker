package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.SubscriptionLinkView;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

    private final RowMapper<SubscriptionLinkView> linkRowMapper = (rs, rowNum) -> SubscriptionLinkView.builder()
            .id(rs.getLong("id"))
            .chatId(rs.getLong("chat_id"))
            .url(rs.getString("url"))
            .tags(null)
            .lastCheckTime(rs.getObject("last_check_time", OffsetDateTime.class))
            .lastUpdateTime(rs.getObject("last_update_time", OffsetDateTime.class))
            .build();

    @Override
    public SubscriptionLinkView save(long chatId, SubscriptionLinkView link) {
        Optional<Long> existingLinkId = findLinkIdByUrl(link.getUrl());
        Long linkId;

        if (existingLinkId.isPresent()) {
            linkId = existingLinkId.orElseThrow(() -> new IllegalArgumentException("linkId не найдена"));
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

        List<String> normalizedTags = normalizeTags(link.getTags());
        if (!normalizedTags.isEmpty()) {
            List<Long> tagIds = new ArrayList<>();
            for (String tagName : normalizedTags) {
                tagIds.add(findOrCreateTag(tagName));
            }

            jdbcTemplate.batchUpdate(
                    "INSERT INTO link_chat_tag (chat_id, link_id, tag_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                    tagIds,
                    tagIds.size(),
                    (ps, tagId) -> {
                        ps.setLong(1, chatId);
                        ps.setLong(2, linkId);
                        ps.setLong(3, tagId);
                    });
        }

        return SubscriptionLinkView.builder()
                .id(linkId)
                .chatId(chatId)
                .url(link.getUrl())
                .tags(normalizedTags)
                .lastCheckTime(link.getLastCheckTime())
                .lastUpdateTime(link.getLastUpdateTime())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionLinkView> findByChatId(long chatId) {
        List<SubscriptionLinkView> links = jdbcTemplate.query("""
            SELECT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id
            FROM links l
            JOIN link_chat lc ON l.id = lc.link_id
            WHERE lc.chat_id = ?
            ORDER BY l.id
            """, linkRowMapper, chatId);

        fillTags(links);
        return links;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionLinkView> findByChatId(long chatId, String tag) {
        if (tag == null || tag.isBlank()) {
            return findByChatId(chatId);
        }

        List<SubscriptionLinkView> links = jdbcTemplate.query("""
            SELECT DISTINCT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id
            FROM links l
            JOIN link_chat lc ON l.id = lc.link_id
            JOIN link_chat_tag lct
                ON lct.chat_id = lc.chat_id
               AND lct.link_id = lc.link_id
            JOIN tags t ON t.id = lct.tag_id
            WHERE lc.chat_id = ? AND t.name = ?
            ORDER BY l.id
            """, linkRowMapper, chatId, tag);

        fillTags(links);
        return links;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionLinkView> findByChatIdAndUrl(long chatId, String url) {
        List<SubscriptionLinkView> links = jdbcTemplate.query("""
            SELECT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id
            FROM links l
            JOIN link_chat lc ON l.id = lc.link_id
            WHERE lc.chat_id = ? AND l.url = ?
            """, linkRowMapper, chatId, url);

        if (links.isEmpty()) {
            return Optional.empty();
        }

        SubscriptionLinkView link = links.getFirst();
        link.setTags(findTagsBySubscription(chatId, link.getId()));
        return Optional.of(link);
    }

    @Override
    public void delete(Long chatId, String url) {
        Optional<Long> linkIdOpt = findLinkIdByUrl(url);
        if (linkIdOpt.isEmpty()) {
            return;
        }

        Long linkId = linkIdOpt.orElseThrow(() -> new IllegalArgumentException("linkId не найдена"));

        jdbcTemplate.update("DELETE FROM link_chat_tag WHERE chat_id = ? AND link_id = ?", chatId, linkId);

        jdbcTemplate.update("DELETE FROM link_chat WHERE chat_id = ? AND link_id = ?", chatId, linkId);

        Integer subscriptionsCount =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM link_chat WHERE link_id = ?", Integer.class, linkId);

        if (subscriptionsCount != null && subscriptionsCount == 0) {
            jdbcTemplate.update("DELETE FROM links WHERE id = ?", linkId);
        }
    }

    @Override
    public void deleteByChatId(Long chatId) {
        List<Long> linkIds =
                jdbcTemplate.queryForList("SELECT link_id FROM link_chat WHERE chat_id = ?", Long.class, chatId);

        jdbcTemplate.update("DELETE FROM link_chat_tag WHERE chat_id = ?", chatId);
        jdbcTemplate.update("DELETE FROM link_chat WHERE chat_id = ?", chatId);

        for (Long linkId : linkIds) {
            Integer subscriptionsCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM link_chat WHERE link_id = ?", Integer.class, linkId);

            if (subscriptionsCount != null && subscriptionsCount == 0) {
                jdbcTemplate.update("DELETE FROM links WHERE id = ?", linkId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionLinkView> findAll() {
        List<SubscriptionLinkView> links = jdbcTemplate.query("""
            SELECT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id
            FROM links l
            JOIN link_chat lc ON l.id = lc.link_id
            ORDER BY l.id, lc.chat_id
            """, linkRowMapper);

        fillTags(links);
        return links;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionLinkView> findAllByUrl(String url) {
        List<SubscriptionLinkView> links = jdbcTemplate.query("""
            SELECT l.id, l.url, l.last_check_time, l.last_update_time, lc.chat_id
            FROM links l
            JOIN link_chat lc ON l.id = lc.link_id
            WHERE l.url = ?
            ORDER BY lc.chat_id
            """, linkRowMapper, url);

        fillTags(links);
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

    private List<String> findTagsBySubscription(long chatId, long linkId) {
        return jdbcTemplate.queryForList("""
            SELECT t.name
            FROM tags t
            JOIN link_chat_tag lct ON t.id = lct.tag_id
            WHERE lct.chat_id = ? AND lct.link_id = ?
            ORDER BY t.name
            """, String.class, chatId, linkId);
    }

    private void fillTags(List<SubscriptionLinkView> links) {
        for (SubscriptionLinkView link : links) {
            link.setTags(findTagsBySubscription(link.getChatId(), link.getId()));
        }
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }
}
