package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.dto.GitHubRepositoryResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowResponse;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.util.LinkParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkUpdateScheduler {
    private final LinkRepository linkRepository;
    private final GitHubClient gitHubClient;
    private final StackOverflowClient stackOverflowClient;
    private final BotClient botClient;
    private final StackoverflowProperties stackoverflowProperties;

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void updateLinks() {
        log.info("Начинаю запланированное обновление ссылок");
        List<Link> allLinks = linkRepository.findAll();
        Map<String, List<Link>> linksByUrl = allLinks.stream()
            .collect(Collectors.groupingBy(Link::getUrl));

        for (Map.Entry<String, List<Link>> entry : linksByUrl.entrySet()) {
            String url = entry.getKey();
            List<Link> linksWithSameUrl = entry.getValue();
            try {
                processUrl(url, linksWithSameUrl);
            } catch (Exception e) {
                log.error("Ошибка в получении ссылки: {}", url, e);
            }
        }
        log.info("Завершение обновления ссылок");
    }

    private void processUrl(String url, List<Link> links) {
        OffsetDateTime lastUpdateFromApi = fetchLastUpdateTime(url);
        if (lastUpdateFromApi == null) {
            return;
        }

        OffsetDateTime currentLastUpdate = links.getFirst().getLastUpdateTime();

        if (lastUpdateFromApi.isAfter(currentLastUpdate)) {
            log.info("Обнаружено обновление для ссылки: {}", url);
            links.forEach(link -> link.setLastUpdateTime(lastUpdateFromApi));

            List<Long> chatIds = links.stream()
                .map(Link::getChatId)
                .collect(Collectors.toList());

            LinkUpdate update = new LinkUpdate(
                links.getFirst().getId(),
                url,
                "Появились новые изменения!",
                chatIds
            );
            botClient.sendUpdate(update);
        } else {
            log.info("Нет обновлений для ссылки: {}", url);
        }
    }

    private OffsetDateTime fetchLastUpdateTime(String url) {
        LinkParser.GitHubData gitHubData = LinkParser.parseGitHub(url);
        if (gitHubData != null) {
            try {
                GitHubRepositoryResponse response = gitHubClient.fetchRepository(gitHubData.owner(), gitHubData.repo());
                return response.updatedAt();
            } catch (Exception e) {
                log.error("GitHub API ошибка в ссылке: {}", url, e);
                return null;
            }
        }

        LinkParser.StackOverflowData stackOverflowData = LinkParser.parseStackOverflow(url);
        if (stackOverflowData != null) {
            try {
                StackOverflowResponse response = stackOverflowClient.fetchQuestions(
                    stackOverflowData.questionId(),
                    "stackoverflow",
                    stackoverflowProperties.getKey(),
                    stackoverflowProperties.getAccessToken()
                );
                if (response.items() != null && !response.items().isEmpty()) {
                    Long lastActivity = response.items().get(0).lastActivityDate();
                    return OffsetDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(lastActivity),
                        ZoneOffset.UTC
                    );
                }
            } catch (Exception e) {
                log.error("StackOverflow API ошибка в ссылке: {}", url, e);
            }
        }

        log.warn("Неправильная ссылка: {}", url);
        return null;
    }
}
