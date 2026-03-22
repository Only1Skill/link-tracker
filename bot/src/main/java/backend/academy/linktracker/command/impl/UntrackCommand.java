package backend.academy.linktracker.command.impl;

import static backend.academy.linktracker.command.UrlValidator.isValidUrl;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.exception.ScrapperClientException;
import backend.academy.linktracker.service.CommandExecutor;
import backend.academy.linktracker.service.state.TrackState;
import backend.academy.linktracker.service.state.UserStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("/untrack")
@RequiredArgsConstructor
public class UntrackCommand implements BotCommandCreation {

    private final ScrapperClient scrapperClient;
    private final UserStateManager userStateManager;
    private final CommandExecutor commandExecutor;

    @Override
    public String execute(UpdateData updateData) {
        Long chatId = updateData.chatId();
        TrackState state = userStateManager.getState(chatId);
        String text = updateData.messageText();

        if (state == TrackState.NONE) {
            userStateManager.setState(chatId, TrackState.AWAITING_UNTRACK_LINK);
            return "Отправьте ссылку, которую хотите перестать отслеживать:";
        }

        if (state == TrackState.AWAITING_UNTRACK_LINK) {
            String url = text.trim();
            if (!isValidUrl(url)) {
                return "Некорректная ссылка. Попробуйте ещё раз или используйте /cancel для отмены.";
            }
            try {
                commandExecutor.executeScrapperCallVoid(() -> scrapperClient.removeLink(chatId, url), chatId);
                userStateManager.clear(chatId);
                return "Ссылка успешно удалена из отслеживаемых!";
            } catch (ScrapperClientException e) {
                userStateManager.clear(chatId);
                return e.getMessage();
            }
        }

        return null;
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
