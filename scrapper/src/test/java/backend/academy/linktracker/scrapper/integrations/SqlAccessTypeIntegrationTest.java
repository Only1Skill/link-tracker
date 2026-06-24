package backend.academy.linktracker.scrapper.integrations;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.repository.ChatStorage;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import backend.academy.linktracker.scrapper.repository.TrackedLinkStorage;
import backend.academy.linktracker.scrapper.repository.jpa.impl.JpaChatStorage;
import backend.academy.linktracker.scrapper.repository.jpa.impl.JpaLinkStorage;
import backend.academy.linktracker.scrapper.repository.jpa.impl.JpaTrackedLinkStorage;
import backend.academy.linktracker.scrapper.repository.sql.SqlChatStorage;
import backend.academy.linktracker.scrapper.repository.sql.SqlLinkStorage;
import backend.academy.linktracker.scrapper.repository.sql.SqlTrackedLinkStorage;
import backend.academy.linktracker.scrapper.test.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.database.access-type=SQL")
class SqlAccessTypeIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ChatStorage chatStorage;

    @Autowired
    private LinkStorage linkStorage;

    @Autowired
    private TrackedLinkStorage trackedLinkStorage;

    @Test
    void shouldUseSqlChatStorage() {
        assertThat(AopProxyUtils.ultimateTargetClass(chatStorage)).isEqualTo(SqlChatStorage.class);
        assertThat(AopProxyUtils.ultimateTargetClass(chatStorage)).isNotEqualTo(JpaChatStorage.class);
    }

    @Test
    void shouldUseSqlLinkStorage() {
        assertThat(AopProxyUtils.ultimateTargetClass(linkStorage)).isEqualTo(SqlLinkStorage.class);
        assertThat(AopProxyUtils.ultimateTargetClass(linkStorage)).isNotEqualTo(JpaLinkStorage.class);
    }

    @Test
    void shouldUseSqlTrackedLinkStorage() {
        assertThat(AopProxyUtils.ultimateTargetClass(trackedLinkStorage)).isEqualTo(SqlTrackedLinkStorage.class);
        assertThat(AopProxyUtils.ultimateTargetClass(trackedLinkStorage)).isNotEqualTo(JpaTrackedLinkStorage.class);
    }
}
