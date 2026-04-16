package backend.academy.linktracker.scrapper.service.formatter;

import backend.academy.linktracker.scrapper.model.LinkEvent;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateMessageFormatter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    private final PreviewExtractor previewExtractor;

    public String format(LinkEvent event) {
        return switch (event.getType()) {
            case GITHUB_ISSUE -> formatGithubIssue(event);
            case GITHUB_PR -> formatGithubPullRequest(event);
            case STACKOVERFLOW_ANSWER -> formatStackOverflowAnswer(event);
            case STACKOVERFLOW_COMMENT -> formatStackOverflowComment(event);
        };
    }

    private String formatGithubIssue(LinkEvent event) {
        return String.format(
                "Обнаружено новое обновление GitHub%n%n"
                        + "Тип: Issue%n"
                        + "Название: %s%n"
                        + "Автор: %s%n"
                        + "Время создания: %s%n"
                        + "Превью описания: %s%n"
                        + "Ссылка: %s",
                safe(event.getTitle()),
                safe(event.getAuthor()),
                formatDateTime(event.getCreatedAt()),
                previewExtractor.makePreview(event.getContent()),
                safe(event.getEventUrl()));
    }

    private String formatGithubPullRequest(LinkEvent event) {
        return String.format(
                "Обнаружено новое обновление GitHub%n%n"
                        + "Тип: Pull Request%n"
                        + "Название: %s%n"
                        + "Автор: %s%n"
                        + "Время создания: %s%n"
                        + "Превью описания: %s%n"
                        + "Ссылка: %s",
                safe(event.getTitle()),
                safe(event.getAuthor()),
                formatDateTime(event.getCreatedAt()),
                previewExtractor.makePreview(event.getContent()),
                safe(event.getEventUrl()));
    }

    private String formatStackOverflowAnswer(LinkEvent event) {
        return String.format(
                "Обнаружено новое обновление StackOverflow%n%n"
                        + "Тип: Ответ%n"
                        + "Тема вопроса: %s%n"
                        + "Автор: %s%n"
                        + "Время создания: %s%n"
                        + "Превью ответа: %s%n"
                        + "Ссылка: %s",
                safe(event.getTitle()),
                safe(event.getAuthor()),
                formatDateTime(event.getCreatedAt()),
                previewExtractor.makePreview(event.getContent()),
                safe(event.getEventUrl()));
    }

    private String formatStackOverflowComment(LinkEvent event) {
        return String.format(
                "Обнаружено новое обновление StackOverflow%n%n"
                        + "Тип: Комментарий%n"
                        + "Тема вопроса: %s%n"
                        + "Автор: %s%n"
                        + "Время создания: %s%n"
                        + "Превью комментария: %s%n"
                        + "Ссылка: %s",
                safe(event.getTitle()),
                safe(event.getAuthor()),
                formatDateTime(event.getCreatedAt()),
                previewExtractor.makePreview(event.getContent()),
                safe(event.getEventUrl()));
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? "unknown" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
