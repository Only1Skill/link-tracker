package backend.academy.linktracker.scrapper.test;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.database.access-type=SQL")
public abstract class SqlIntegrationTestBase extends IntegrationTestBase {}
