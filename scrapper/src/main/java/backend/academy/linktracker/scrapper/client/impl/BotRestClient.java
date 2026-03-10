package backend.academy.linktracker.scrapper.client.impl;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class BotRestClient implements BotClient {
    private final RestClient botRestClient;

    @Override
    public void sendUpdate(LinkUpdate update) {
        try {
            botRestClient.post()
                .uri("/updates")
                .body(update)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("Bot returned error: {} - {}", res.getStatusCode(), res.getStatusText());
                })
                .toBodilessEntity();
            log.info("Update sent to bot for link id: {}", update.id());
        } catch (Exception e) {
            log.error("Failed to send update to bot", e);
        }
    }
}
