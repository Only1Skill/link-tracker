package backend.academy.linktracker.client.impl;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.configuration.ScrapperProperties;
import backend.academy.linktracker.exception.ScrapperClientException;
import backend.academy.linktracker.dto.AddLinkRequest;
import backend.academy.linktracker.dto.LinkResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Optional;

@Component
public class ScrapperRestClient implements ScrapperClient {
    private final RestClient restClient;

    public ScrapperRestClient(ScrapperProperties properties) {
        this.restClient = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .build();
    }


    @Override
    public void registerChat(long chatId) {
        restClient.post()
            .uri("/tg-chat/{id}", chatId)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                throw new ScrapperClientException("Ошибка регистрации чата: " + res.getStatusCode());
            })
            .toBodilessEntity();
    }


    @Override
    public LinkResponse addLink(long chatId, AddLinkRequest request) {
        return restClient.post()
            .uri("/api/links")
            .header("Tg-Chat-Id", String.valueOf(chatId))
            .body(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                throw new ScrapperClientException("Ошибка добавления ссылки: " + res.getStatusCode());
            })
            .body(LinkResponse.class);
    }

    @Override
    public List<LinkResponse> getLinks(long chatId, String tag) {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/links")
                .queryParamIfPresent("tag", Optional.ofNullable(tag))
                .build())
            .header("Tg-Chat-Id", String.valueOf(chatId))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                throw new ScrapperClientException("Ошибка получения ссылок: " + res.getStatusCode());
            })
            .body(new ParameterizedTypeReference<List<LinkResponse>>() {
            });
    }

    @Override
    public void removeLink(long chatId, String url) {
        AddLinkRequest request = new AddLinkRequest();
        request.setLink(url);
        restClient.method(HttpMethod.DELETE)
            .uri("/api/links")
            .header("Tg-Chat-Id", String.valueOf(chatId))
            .body(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                throw new ScrapperClientException("Ошибка удаления ссылки: " + res.getStatusCode());
            })
            .toBodilessEntity();
    }

    @Override
    public void deleteChat(long chatId) {
        restClient.delete()
            .uri("/tg-chat/{id}", chatId)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                throw new ScrapperClientException("Ошибка удаления чата: " + res.getStatusCode());
            })
            .toBodilessEntity();
    }
}
