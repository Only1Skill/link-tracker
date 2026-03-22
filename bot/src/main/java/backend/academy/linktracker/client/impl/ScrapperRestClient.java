package backend.academy.linktracker.client.impl;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.configuration.ScrapperProperties;
import backend.academy.linktracker.dto.AddLinkRequest;
import backend.academy.linktracker.dto.ApiErrorResponse;
import backend.academy.linktracker.dto.LinkResponse;
import backend.academy.linktracker.exception.ScrapperClientException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Component
public class ScrapperRestClient implements ScrapperClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ScrapperRestClient(ScrapperProperties properties, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void registerChat(long chatId) {
        restClient
                .post()
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> handleError(res))
                .toBodilessEntity();
    }

    @Override
    public LinkResponse addLink(long chatId, AddLinkRequest request) {
        return restClient
                .post()
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> handleError(res))
                .body(LinkResponse.class);
    }

    @Override
    public List<LinkResponse> getLinks(long chatId, String tag) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/links")
                        .queryParamIfPresent("tag", Optional.ofNullable(tag))
                        .build())
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> handleError(res))
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public void removeLink(long chatId, String url) {
        AddLinkRequest request = new AddLinkRequest(url, null);
        restClient
                .method(HttpMethod.DELETE)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> handleError(res))
                .toBodilessEntity();
    }

    @Override
    public void deleteChat(long chatId) {
        restClient
                .delete()
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> handleError(res))
                .toBodilessEntity();
    }

    void handleError(ClientHttpResponse res) {
        try {
            String errorBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
            ApiErrorResponse errorResponse = objectMapper.readValue(errorBody, ApiErrorResponse.class);
            throw new ScrapperClientException(errorResponse.message());
        } catch (IOException e) {
            throw new ScrapperClientException("Не удалось прочитать ответ от сервера", e);
        }
    }
}
