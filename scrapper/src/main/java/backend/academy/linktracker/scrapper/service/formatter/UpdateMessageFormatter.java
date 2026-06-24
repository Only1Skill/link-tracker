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
        return formatMessage(event, "GitHub", "Issue", "Название", "Превью описания");
    }

    private String formatGithubPullRequest(LinkEvent event) {
        return formatMessage(event, "GitHub", "Pull Request", "Название", "Превью описания");
    }

    private String formatStackOverflowAnswer(LinkEvent event) {
        return formatMessage(event, "StackOverflow", "Ответ", "Тема вопроса", "Превью ответа");
    }

    private String formatStackOverflowComment(LinkEvent event) {
        return formatMessage(event, "StackOverflow", "Комментарий", "Тема вопроса", "Превью комментария");
    }

    private String formatMessage(LinkEvent event, String source, String type, String titleLabel, String previewLabel) {
        return String.format(
                "Обнаружено новое обновление %s%n%n"
                        + "Тип: %s%n"
                        + "%s: %s%n"
                        + "Автор: %s%n"
                        + "Время создания: %s%n"
                        + "%s: %s%n"
                        + "Ссылка: %s",
                source,
                type,
                titleLabel,
                safe(event.getTitle()),
                safe(event.getAuthor()),
                formatDateTime(event.getCreatedAt()),
                previewLabel,
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
