package backend.academy.linktracker.scrapper.integrations;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.repository.ChatStorage;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import backend.academy.linktracker.scrapper.repository.TagStorage;
import backend.academy.linktracker.scrapper.repository.TrackedLinkStorage;
import backend.academy.linktracker.scrapper.repository.sql.SqlChatStorage;
import backend.academy.linktracker.scrapper.repository.sql.SqlLinkStorage;
import backend.academy.linktracker.scrapper.repository.sql.SqlTagStorage;
import backend.academy.linktracker.scrapper.repository.sql.SqlTrackedLinkStorage;
import backend.academy.linktracker.scrapper.test.SqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SqlAccessTypeIntegrationTest extends SqlIntegrationTestBase {

    @Autowired
    private ChatStorage chatStorage;

    @Autowired
    private LinkStorage linkStorage;

    @Autowired
    private TagStorage tagStorage;

    @Autowired
    private TrackedLinkStorage trackedLinkStorage;

    @Test
    void shouldUseSqlImplementations() {
        assertThat(chatStorage).isInstanceOf(SqlChatStorage.class);
        assertThat(linkStorage).isInstanceOf(SqlLinkStorage.class);
        assertThat(tagStorage).isInstanceOf(SqlTagStorage.class);
        assertThat(trackedLinkStorage).isInstanceOf(SqlTrackedLinkStorage.class);
    }
}
