package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.StackOverflowResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "https://api.stackexchange.com/2.3")
public interface StackOverflowClient {

    @GetExchange("/questions/{ids}")
    StackOverflowResponse fetchQuestions(
            @PathVariable String ids,
            @RequestParam("site") String site,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "access_token", required = false) String accessToken);
}
