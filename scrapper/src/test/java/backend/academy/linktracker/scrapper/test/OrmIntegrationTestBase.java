package backend.academy.linktracker.scrapper.test;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.database.access-type=ORM")
public abstract class OrmIntegrationTestBase extends IntegrationTestBase {}
