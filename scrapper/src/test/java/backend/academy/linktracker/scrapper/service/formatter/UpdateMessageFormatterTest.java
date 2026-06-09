package backend.academy.linktracker.scrapper.service.formatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.model.LinkEvent;
import backend.academy.linktracker.scrapper.model.LinkEventType;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateMessageFormatterTest {

    @Mock
    private PreviewExtractor previewExtractor;

    private UpdateMessageFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new UpdateMessageFormatter(previewExtractor);
    }

    @Test
    void format_shouldContainRequiredFields_forGithubIssue() {
        LinkEvent event = LinkEvent.builder()
                .type(LinkEventType.GITHUB_ISSUE)
                .title("Bug in parser")
                .author("alice")
                .createdAt(OffsetDateTime.parse("2026-04-16T10:15:30Z"))
                .content("raw body")
                .eventUrl("https://github.com/test/repo/issues/1")
                .build();

        when(previewExtractor.makePreview("raw body")).thenReturn("preview text");

        String message = formatter.format(event);

        assertThat(message)
                .contains("Bug in parser")
                .contains("alice")
                .contains("preview text")
                .contains("https://github.com/test/repo/issues/1")
                .contains("2026-04-16");
    }

    @Test
    void format_shouldContainRequiredFields_forGithubPullRequest() {
        LinkEvent event = LinkEvent.builder()
                .type(LinkEventType.GITHUB_PR)
                .title("Add new API")
                .author("bob")
                .createdAt(OffsetDateTime.parse("2026-04-16T11:00:00Z"))
                .content("raw body")
                .eventUrl("https://github.com/test/repo/pull/2")
                .build();

        when(previewExtractor.makePreview("raw body")).thenReturn("pr preview");

        String message = formatter.format(event);

        assertThat(message)
                .contains("Add new API")
                .contains("bob")
                .contains("pr preview")
                .contains("https://github.com/test/repo/pull/2")
                .contains("2026-04-16");
    }

    @Test
    void format_shouldContainRequiredFields_forStackOverflowAnswer() {
        LinkEvent event = LinkEvent.builder()
                .type(LinkEventType.STACKOVERFLOW_ANSWER)
                .title("How to test Spring service?")
                .author("charlie")
                .createdAt(OffsetDateTime.parse("2026-04-16T12:00:00Z"))
                .content("answer body")
                .eventUrl("https://stackoverflow.com/a/123")
                .build();

        when(previewExtractor.makePreview("answer body")).thenReturn("answer preview");

        String message = formatter.format(event);

        assertThat(message)
                .contains("How to test Spring service?")
                .contains("charlie")
                .contains("answer preview")
                .contains("https://stackoverflow.com/a/123")
                .contains("2026-04-16");
    }

    @Test
    void format_shouldContainRequiredFields_forStackOverflowComment() {
        LinkEvent event = LinkEvent.builder()
                .type(LinkEventType.STACKOVERFLOW_COMMENT)
                .title("How to test Spring service?")
                .author("david")
                .createdAt(OffsetDateTime.parse("2026-04-16T12:30:00Z"))
                .content("comment body")
                .eventUrl("https://stackoverflow.com/questions/1#comment2_1")
                .build();

        when(previewExtractor.makePreview("comment body")).thenReturn("comment preview");

        String message = formatter.format(event);

        assertThat(message)
                .contains("How to test Spring service?")
                .contains("david")
                .contains("comment preview")
                .contains("https://stackoverflow.com/questions/1#comment2_1")
                .contains("2026-04-16");
    }
}
