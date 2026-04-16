package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.test.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractLinkCrudIntegrationTest extends IntegrationTestBase {

    protected static final long CHAT_ID = 1001L;
    protected static final String URL = "https://github.com/test-owner/test-repo";

    @Autowired
    protected ChatService chatService;

    @BeforeEach
    void registerChat() {
        chatService.register(CHAT_ID);
    }

    @Test
    void addLink_shouldPersistLinkAndSubscription() {
        addLink(CHAT_ID, URL);

        Integer linksCount =
                jdbcTemplate.queryForObject("select count(*) from links where url = ?", Integer.class, URL);

        Integer subscriptionsCount = jdbcTemplate.queryForObject("""
                select count(*)
                from link_chat lc
                join links l on l.id = lc.link_id
                where lc.chat_id = ? and l.url = ?
                """, Integer.class, CHAT_ID, URL);

        assertThat(linksCount).isEqualTo(1);
        assertThat(subscriptionsCount).isEqualTo(1);
    }

    @Test
    void removeLink_shouldDeleteSubscription() {
        addLink(CHAT_ID, URL);

        removeLink(CHAT_ID, URL);

        Integer subscriptionsCount = jdbcTemplate.queryForObject("""
                select count(*)
                from link_chat lc
                join links l on l.id = lc.link_id
                where lc.chat_id = ? and l.url = ?
                """, Integer.class, CHAT_ID, URL);

        assertThat(subscriptionsCount).isEqualTo(0);
    }

    @Test
    void addDuplicateLink_shouldThrowAndKeepSingleSubscription() {
        addLink(CHAT_ID, URL);

        assertThatThrownBy(() -> addLink(CHAT_ID, URL)).isInstanceOf(RuntimeException.class);

        Integer linksCount =
                jdbcTemplate.queryForObject("select count(*) from links where url = ?", Integer.class, URL);

        Integer subscriptionsCount = jdbcTemplate.queryForObject("""
                select count(*)
                from link_chat lc
                join links l on l.id = lc.link_id
                where lc.chat_id = ? and l.url = ?
                """, Integer.class, CHAT_ID, URL);

        assertThat(linksCount).isEqualTo(1);
        assertThat(subscriptionsCount).isEqualTo(1);
    }

    protected abstract void addLink(long chatId, String url);

    protected abstract void removeLink(long chatId, String url);
}
