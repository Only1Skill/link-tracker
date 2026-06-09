package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.StackOverflowAnswerResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowApiResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowCommentResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowQuestionResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface StackOverflowClient {

    @GetExchange("/questions/{ids}")
    StackOverflowApiResponse<StackOverflowQuestionResponse> fetchQuestions(
            @PathVariable String ids,
            @RequestParam("site") String site,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "access_token", required = false) String accessToken);

    @GetExchange("/questions/{ids}/answers")
    StackOverflowApiResponse<StackOverflowAnswerResponse> fetchQuestionAnswers(
            @PathVariable String ids,
            @RequestParam("site") String site,
            @RequestParam("sort") String sort,
            @RequestParam("fromdate") Long fromDate,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "access_token", required = false) String accessToken);

    @GetExchange("/questions/{ids}/comments")
    StackOverflowApiResponse<StackOverflowCommentResponse> fetchQuestionComments(
            @PathVariable String ids,
            @RequestParam("site") String site,
            @RequestParam("sort") String sort,
            @RequestParam("fromdate") Long fromDate,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "access_token", required = false) String accessToken);
}
