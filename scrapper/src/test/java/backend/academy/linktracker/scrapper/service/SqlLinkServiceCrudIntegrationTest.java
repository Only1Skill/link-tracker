package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.database.access-type=SQL")
class SqlLinkServiceCrudIntegrationTest extends AbstractLinkCrudIntegrationTest {

    @Autowired
    private LinkService linkService;

    @Override
    protected void addLink(long chatId, String url) {
        linkService.addLink(chatId, new AddLinkRequest(url, java.util.List.of()));
    }

    @Override
    protected void removeLink(long chatId, String url) {
        linkService.removeLink(chatId, url);
    }
}
