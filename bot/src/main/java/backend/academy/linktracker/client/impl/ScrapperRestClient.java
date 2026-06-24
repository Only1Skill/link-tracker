package backend.academy.linktracker.client.impl;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.configuration.ScrapperProperties;
import backend.academy.linktracker.dto.AddLinkRequest;
import backend.academy.linktracker.dto.ApiErrorResponse;
import backend.academy.linktracker.dto.LinkResponse;
import backend.academy.linktracker.exception.ScrapperClientException;
import backend.academy.linktracker.resilience.HttpResilienceExecutor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Component
public class ScrapperRestClient implements ScrapperClient {

    private static final String CLIENT_NAME = "scrapper";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final HttpResilienceExecutor resilienceExecutor;

    @Autowired
    public ScrapperRestClient(
            ScrapperProperties properties,
            ObjectMapper objectMapper,
            ClientHttpRequestFactory clientHttpRequestFactory,
            HttpResilienceExecutor resilienceExecutor) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(clientHttpRequestFactory)
                .build();
        this.objectMapper = objectMapper;
        this.resilienceExecutor = resilienceExecutor;
    }

    ScrapperRestClient(ScrapperProperties properties, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
        this.objectMapper = objectMapper;
        this.resilienceExecutor = null;
    }

    @Override
    public void registerChat(long chatId) {
        executeVoid(() -> restClient
                .post()
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> handleError(res))
                .toBodilessEntity());
    }

    @Override
    public LinkResponse addLink(long chatId, AddLinkRequest request) {
        return execute(() -> restClient
                .post()
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> handleError(res))
                .body(LinkResponse.class));
    }

    @Override
    public List<LinkResponse> getLinks(long chatId, String tag) {
        return execute(() -> restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/links")
                        .queryParamIfPresent("tag", Optional.ofNullable(tag))
                        .build())
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> handleError(res))
                .body(new ParameterizedTypeReference<>() {}));
    }

    @Override
    public void removeLink(long chatId, String url) {
        AddLinkRequest request = new AddLinkRequest(url, null);
        executeVoid(() -> restClient
                .method(HttpMethod.DELETE)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> handleError(res))
                .toBodilessEntity());
    }

    @Override
    public void deleteChat(long chatId) {
        executeVoid(() -> restClient
                .delete()
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> handleError(res))
                .toBodilessEntity());
    }

    void handleError(ClientHttpResponse res) {
        try {
            int statusCode = res.getStatusCode().value();
            String errorBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
            ApiErrorResponse errorResponse = objectMapper.readValue(errorBody, ApiErrorResponse.class);
            throw new ScrapperClientException(errorResponse.message(), statusCode);
        } catch (IOException e) {
            throw new ScrapperClientException("Не удалось прочитать ответ от сервера", e);
        }
    }

    private <T> T execute(Supplier<T> request) {
        if (resilienceExecutor == null) {
            return request.get();
        }
        return resilienceExecutor.execute(CLIENT_NAME, request);
    }

    private void executeVoid(Runnable request) {
        if (resilienceExecutor == null) {
            request.run();
            return;
        }
        resilienceExecutor.executeVoid(CLIENT_NAME, request);
    }
}
