package backend.academy.linktracker.scrapper.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.dto.GitHubRepositoryResponse;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkStorage;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = "app.scheduler.interval=-1")
class LinkUpdateSchedulerTest {

    @Autowired
    private LinkStorage linkStorage;

    @MockitoBean
    private BotClient botClient;

    @MockitoBean
    private GitHubClient gitHubClient;

    @MockitoBean
    private StackOverflowClient stackOverflowClient;

    @MockitoBean
    private TaskScheduler taskScheduler;

    @Autowired
    private LinkUpdateScheduler scheduler;

    @Test
    void updateLinks_detectsGitHubUpdateAndSendsMessage() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime later = now.plusHours(1);

        Link link = Link.builder()
                .id(1L)
                .chatId(123L)
                .url("https://github.com/owner/repo")
                .lastUpdateTime(now)
                .build();
        linkStorage.save(link.getChatId(), link);
        when(gitHubClient.fetchRepository("owner", "repo")).thenReturn(new GitHubRepositoryResponse(later));
        scheduler.updateLinks();
        verify(botClient)
                .sendUpdate(argThat(update -> update.url().equals("https://github.com/owner/repo")
                        && update.tgChatIds().contains(123L)
                        && update.description().equals("Появились новые изменения!")));
    }

    @Test
    void updateLinks_whenNoUpdate_doesNotSendUpdate() {
        OffsetDateTime now = OffsetDateTime.now();

        Link link = Link.builder()
                .chatId(123L)
                .url("https://github.com/owner/repo")
                .lastUpdateTime(now)
                .build();
        linkStorage.save(link.getChatId(), link);

        when(gitHubClient.fetchRepository("owner", "repo")).thenReturn(new GitHubRepositoryResponse(now));

        scheduler.updateLinks();

        verify(botClient, never()).sendUpdate(any());
    }
}
