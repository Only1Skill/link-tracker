package backend.academy.linktracker.scrapper.service.detector.impl;

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
import backend.academy.linktracker.scrapper.service.detector.LinkUpdateDetector;
import backend.academy.linktracker.scrapper.util.LinkParser;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StackOverflowLinkUpdateDetector implements LinkUpdateDetector {

    private static final String SORT_BY_CREATION = "creation";

    private final StackOverflowClient stackOverflowClient;
    private final StackoverflowProperties stackoverflowProperties;

    @Override
    public boolean supports(String url) {
        return LinkParser.parseStackOverflow(url) != null;
    }

    @Override
    public List<LinkEvent> detectUpdates(TrackedLink trackedLink) {
        LinkParser.StackOverflowData stackOverflowData = LinkParser.parseStackOverflow(trackedLink.getUrl());
        if (stackOverflowData == null) {
            throw new IllegalArgumentException("StackOverflow не поддерживает данную ссылку: " + trackedLink.getUrl());
        }

        String questionId = stackOverflowData.questionId();
        long fromDate = trackedLink.getLastUpdateTime().toInstant().getEpochSecond();

        StackOverflowApiResponse<StackOverflowQuestionResponse> questionResponse = stackOverflowClient.fetchQuestions(
                questionId,
                stackoverflowProperties.getSite(),
                stackoverflowProperties.getFilter(),
                stackoverflowProperties.getKey(),
                stackoverflowProperties.getAccessToken());

        if (questionResponse.items().isEmpty()) {
            return List.of();
        }

        String questionTitle = safe(questionResponse.items().getFirst().title());

        StackOverflowApiResponse<StackOverflowAnswerResponse> answersResponse =
                stackOverflowClient.fetchQuestionAnswers(
                        questionId,
                        stackoverflowProperties.getSite(),
                        SORT_BY_CREATION,
                        fromDate,
                        stackoverflowProperties.getFilter(),
                        stackoverflowProperties.getKey(),
                        stackoverflowProperties.getAccessToken());

        StackOverflowApiResponse<StackOverflowCommentResponse> commentsResponse =
                stackOverflowClient.fetchQuestionComments(
                        questionId,
                        stackoverflowProperties.getSite(),
                        SORT_BY_CREATION,
                        fromDate,
                        stackoverflowProperties.getFilter(),
                        stackoverflowProperties.getKey(),
                        stackoverflowProperties.getAccessToken());

        Stream<LinkEvent> answerEvents = answersResponse.items().stream()
                .filter(answer -> answer.creationDate() != null)
                .filter(answer -> answer.creationDate() > fromDate)
                .map(answer -> mapAnswerToEvent(trackedLink, questionTitle, answer));

        Stream<LinkEvent> commentEvents = commentsResponse.items().stream()
                .filter(comment -> comment.creationDate() != null)
                .filter(comment -> comment.creationDate() > fromDate)
                .map(comment -> mapCommentToEvent(trackedLink, questionTitle, comment));

        return Stream.concat(answerEvents, commentEvents)
                .sorted(Comparator.comparing(LinkEvent::getCreatedAt))
                .toList();
    }

    private LinkEvent mapAnswerToEvent(
            TrackedLink trackedLink, String questionTitle, StackOverflowAnswerResponse answer) {
        return LinkEvent.builder()
                .linkId(trackedLink.getId())
                .url(trackedLink.getUrl())
                .type(LinkEventType.STACKOVERFLOW_ANSWER)
                .title(questionTitle)
                .author(resolveAuthor(answer.owner()))
                .createdAt(toOffsetDateTime(answer.creationDate()))
                .content(safe(answer.body()))
                .eventUrl(safe(answer.link()))
                .build();
    }

    private LinkEvent mapCommentToEvent(
            TrackedLink trackedLink, String questionTitle, StackOverflowCommentResponse comment) {
        return LinkEvent.builder()
                .linkId(trackedLink.getId())
                .url(trackedLink.getUrl())
                .type(LinkEventType.STACKOVERFLOW_COMMENT)
                .title(questionTitle)
                .author(resolveAuthor(comment.owner()))
                .createdAt(toOffsetDateTime(comment.creationDate()))
                .content(safe(comment.body()))
                .eventUrl(safe(comment.link()))
                .build();
    }

    private OffsetDateTime toOffsetDateTime(Long epochSeconds) {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }

    private String resolveAuthor(StackOverflowOwnerResponse owner) {
        return owner == null
                        || owner.displayName() == null
                        || owner.displayName().isBlank()
                ? "unknown"
                : owner.displayName();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
