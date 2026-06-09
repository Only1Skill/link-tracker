package backend.academy.linktracker.scrapper.service.detector.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowAnswerResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowApiResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowCommentResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowOwnerResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowQuestionResponse;
import backend.academy.linktracker.scrapper.model.LinkEvent;
import backend.academy.linktracker.scrapper.model.LinkEventType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StackOverflowLinkUpdateDetectorTest {

    @Mock
    private StackOverflowClient stackOverflowClient;

    private StackOverflowLinkUpdateDetector detector;
    private StackoverflowProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StackoverflowProperties();
        properties.setBaseUrl("https://api.stackexchange.com/2.3");
        properties.setSite("stackoverflow");
        properties.setFilter("withbody");
        properties.setKey("key");
        properties.setAccessToken("token");

        detector = new StackOverflowLinkUpdateDetector(stackOverflowClient, properties);
    }

    @Test
    void supports_shouldReturnTrue_forStackOverflowUrl() {
        assertThat(detector.supports("https://stackoverflow.com/questions/12345/test-question"))
                .isTrue();
    }

    @Test
    void supports_shouldReturnFalse_forUnsupportedUrl() {
        assertThat(detector.supports("https://github.com/test-owner/test-repo")).isFalse();
    }

    @Test
    void detectUpdates_shouldMapAnswersAndComments_filterOldOnes_andSortByCreatedAt() {
        OffsetDateTime lastCheckTime = OffsetDateTime.parse("2026-04-16T10:00:00Z");
        long fromDate = lastCheckTime.toEpochSecond();

        TrackedLink trackedLink = TrackedLink.builder()
                .id(10L)
                .url("https://stackoverflow.com/questions/12345/test-question")
                .lastCheckTime(lastCheckTime)
                .lastUpdateTime(lastCheckTime)
                .build();

        StackOverflowQuestionResponse question = new StackOverflowQuestionResponse(
                12345L,
                "How to write tests?",
                null,
                fromDate - 1000,
                fromDate - 500,
                "https://stackoverflow.com/questions/12345/test-question",
                new StackOverflowOwnerResponse("question-owner"));

        StackOverflowAnswerResponse oldAnswer = new StackOverflowAnswerResponse(
                1L,
                12345L,
                null,
                "Old answer",
                fromDate - 10,
                fromDate - 10,
                "https://stackoverflow.com/a/1",
                new StackOverflowOwnerResponse("old-answer-author"));

        StackOverflowAnswerResponse newAnswer = new StackOverflowAnswerResponse(
                2L,
                12345L,
                null,
                "New answer",
                fromDate + 10,
                fromDate + 10,
                "https://stackoverflow.com/a/2",
                new StackOverflowOwnerResponse("new-answer-author"));

        StackOverflowCommentResponse newComment = new StackOverflowCommentResponse(
                3L,
                12345L,
                "New comment",
                fromDate + 20,
                "https://stackoverflow.com/questions/12345/test-question#comment3_12345",
                new StackOverflowOwnerResponse("new-comment-author"));

        when(stackOverflowClient.fetchQuestions("12345", "stackoverflow", "withbody", "key", "token"))
                .thenReturn(new StackOverflowApiResponse<>(List.of(question)));

        when(stackOverflowClient.fetchQuestionAnswers(
                        "12345", "stackoverflow", "creation", fromDate, "withbody", "key", "token"))
                .thenReturn(new StackOverflowApiResponse<>(List.of(oldAnswer, newAnswer)));

        when(stackOverflowClient.fetchQuestionComments(
                        "12345", "stackoverflow", "creation", fromDate, "withbody", "key", "token"))
                .thenReturn(new StackOverflowApiResponse<>(List.of(newComment)));

        List<LinkEvent> events = detector.detectUpdates(trackedLink);

        assertThat(events).hasSize(2);

        assertThat(events.getFirst().getType()).isEqualTo(LinkEventType.STACKOVERFLOW_ANSWER);
        assertThat(events.getFirst().getTitle()).isEqualTo("How to write tests?");
        assertThat(events.getFirst().getAuthor()).isEqualTo("new-answer-author");
        assertThat(events.getFirst().getContent()).isEqualTo("New answer");

        assertThat(events.get(1).getType()).isEqualTo(LinkEventType.STACKOVERFLOW_COMMENT);
        assertThat(events.get(1).getTitle()).isEqualTo("How to write tests?");
        assertThat(events.get(1).getAuthor()).isEqualTo("new-comment-author");
        assertThat(events.get(1).getContent()).isEqualTo("New comment");

        verify(stackOverflowClient).fetchQuestions("12345", "stackoverflow", "withbody", "key", "token");
    }

    @Test
    void detectUpdates_shouldReturnEmptyList_whenQuestionNotFound() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(10L)
                .url("https://stackoverflow.com/questions/12345/test-question")
                .lastCheckTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .lastUpdateTime(OffsetDateTime.parse("2026-04-16T10:00:00Z"))
                .build();

        when(stackOverflowClient.fetchQuestions("12345", "stackoverflow", "withbody", "key", "token"))
                .thenReturn(new StackOverflowApiResponse<>(List.of()));

        List<LinkEvent> events = detector.detectUpdates(trackedLink);

        assertThat(events).isEmpty();
    }

    @Test
    void detectUpdates_shouldThrow_whenUrlIsUnsupported() {
        TrackedLink trackedLink = TrackedLink.builder()
                .id(10L)
                .url("https://example.com/not-supported")
                .lastCheckTime(OffsetDateTime.now())
                .lastUpdateTime(OffsetDateTime.now())
                .build();

        assertThatThrownBy(() -> detector.detectUpdates(trackedLink))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не поддерживает");
    }
}
