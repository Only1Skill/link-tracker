package backend.academy.linktracker.command.impl;

import static backend.academy.linktracker.command.UrlValidator.isValidUrl;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.service.CommandExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("/untrack")
@RequiredArgsConstructor
public class UntrackCommand implements BotCommandCreation {

    private final ScrapperClient scrapperClient;
    private final CommandExecutor commandExecutor;

    @Override
    public String execute(UpdateData updateData) {
        Long chatId = updateData.chatId();
        String text = updateData.messageText();

        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return "Пожалуйста, укажите ссылку для удаления, например: /untrack https://github.com/user/repo";
        }

        String url = parts[1].trim();

        if (!isValidUrl(url)) {
            return "Некорректная ссылка. Убедитесь, что она начинается с http:// или https://";
        }

        commandExecutor.executeScrapperCallVoid(
                () -> scrapperClient.removeLink(chatId, url), chatId, "Ошибка при удалении ссылки. Попробуйте позже.");

        return "Ссылка успешно удалена!";
    }

    @Override
    public String getCommand() {
        return "/untrack";
    }

    @Override
    public String getDescription() {
        return "Прекратить отслеживание ссылки";
    }
}
