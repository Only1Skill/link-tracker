package backend.academy.linktracker.scrapper.client.impl;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class BotRestClient implements BotClient {
    private final RestClient botRestClient;

    @Override
    public void sendUpdate(LinkUpdate update) {
        try {
            botRestClient
                    .post()
                    .uri("/updates")
                    .body(update)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (_, res) -> log.error(
                                    "Бот вернул ошибку: {} - {}",
                                    res.getStatusCode(),
                                    LogSanitizer.sanitize(res.getStatusText())))
                    .toBodilessEntity();
            log.info("Обновление, отправленное боту для получения идентификатора ссылки: {}", update.id());
        } catch (Exception e) {
            log.error("Не удалось отправить обновление боту", e);
        }
    }
}
