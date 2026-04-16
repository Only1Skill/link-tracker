package backend.academy.linktracker.scrapper.integrations;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.repository.ChatStorage;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import backend.academy.linktracker.scrapper.repository.TagStorage;
import backend.academy.linktracker.scrapper.repository.TrackedLinkStorage;
import backend.academy.linktracker.scrapper.repository.jpa.impl.JpaChatStorage;
import backend.academy.linktracker.scrapper.repository.jpa.impl.JpaLinkStorage;
import backend.academy.linktracker.scrapper.repository.jpa.impl.JpaTagStorage;
import backend.academy.linktracker.scrapper.repository.jpa.impl.JpaTrackedLinkStorage;
import backend.academy.linktracker.scrapper.test.OrmIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrmAccessTypeIntegrationTest extends OrmIntegrationTestBase {

    @Autowired
    private ChatStorage chatStorage;

    @Autowired
    private LinkStorage linkStorage;

    @Autowired
    private TagStorage tagStorage;

    @Autowired
    private TrackedLinkStorage trackedLinkStorage;

    @Test
    void shouldUseOrmImplementations() {
        assertThat(chatStorage).isInstanceOf(JpaChatStorage.class);
        assertThat(linkStorage).isInstanceOf(JpaLinkStorage.class);
        assertThat(tagStorage).isInstanceOf(JpaTagStorage.class);
        assertThat(trackedLinkStorage).isInstanceOf(JpaTrackedLinkStorage.class);
    }
}
