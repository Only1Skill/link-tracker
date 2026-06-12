package backend.academy.linktracker.scrapper.client.impl;

import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowAnswerResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowApiResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowCommentResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowQuestionResponse;
import backend.academy.linktracker.scrapper.resilience.HttpResilienceExecutor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResilientStackOverflowClient implements StackOverflowClient {

    private static final String CLIENT_NAME = "stackoverflow";

    private final StackOverflowClient delegate;
    private final HttpResilienceExecutor executor;

    @Override
    public StackOverflowApiResponse<StackOverflowQuestionResponse> fetchQuestions(
            String ids, String site, String filter, String key, String accessToken) {
        return executor.execute(CLIENT_NAME, () -> delegate.fetchQuestions(ids, site, filter, key, accessToken));
    }

    @Override
    public StackOverflowApiResponse<StackOverflowAnswerResponse> fetchQuestionAnswers(
            String ids, String site, String sort, Long fromDate, String filter, String key, String accessToken) {
        return executor.execute(
                CLIENT_NAME, () -> delegate.fetchQuestionAnswers(ids, site, sort, fromDate, filter, key, accessToken));
    }

    @Override
    public StackOverflowApiResponse<StackOverflowCommentResponse> fetchQuestionComments(
            String ids, String site, String sort, Long fromDate, String filter, String key, String accessToken) {
        return executor.execute(
                CLIENT_NAME, () -> delegate.fetchQuestionComments(ids, site, sort, fromDate, filter, key, accessToken));
    }
}
