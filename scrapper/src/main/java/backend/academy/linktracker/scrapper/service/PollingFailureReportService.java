package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.ChatNotification;
import backend.academy.linktracker.scrapper.model.LinkProcessingError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollingFailureReportService {

    private final BotClient botClient;

    public void sendFailureReport(List<LinkProcessingError> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        Map<Long, List<LinkProcessingError>> errorsByChat = new HashMap<>();

        for (LinkProcessingError error : errors) {
            if (error.getChatIds() == null || error.getChatIds().isEmpty()) {
                continue;
            }

            for (Long chatId : error.getChatIds()) {
                errorsByChat.computeIfAbsent(chatId, _ -> new ArrayList<>()).add(error);
            }
        }

        for (Map.Entry<Long, List<LinkProcessingError>> entry : errorsByChat.entrySet()) {
            Long chatId = entry.getKey();
            List<LinkProcessingError> chatErrors = entry.getValue();

            StringBuilder message = new StringBuilder("Не удалось проверить некоторые ссылки:\n");

            for (LinkProcessingError error : chatErrors) {
                message.append("- ")
                        .append(error.getUrl())
                        .append(" — ")
                        .append(error.getReason())
                        .append("\n");
            }

            botClient.sendNotification(new ChatNotification(message.toString().trim(), List.of(chatId)));
        }

        log.info("Отправлены отчёты по ссылкам с ошибками, users={}", errorsByChat.size());
    }
}
