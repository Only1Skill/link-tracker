package backend.academy.linktracker.command.impl;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.client.impl.ScrapperRestClient;
import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.dto.AddLinkRequest;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.dto.LinkResponse;
import backend.academy.linktracker.service.CommandExecutor;
import backend.academy.linktracker.service.state.TrackState;
import backend.academy.linktracker.service.state.UserStateManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static backend.academy.linktracker.command.UrlValidator.isValidUrl;

@Component("/track")
@RequiredArgsConstructor
public class TrackCommand implements BotCommandCreation {
    private final CommandExecutor commandExecutor;
    private final UserStateManager userStateManager;
    private final ScrapperClient scrapperClient;

    @Override
    public String execute(UpdateData updateData) {
        Long chatId = updateData.chatId();
        TrackState state = userStateManager.getState(chatId);
        String text = updateData.messageText();

        switch (state) {
            case NONE:
                userStateManager.setState(chatId, TrackState.AWAITING_LINK);
                return "Отправьте ссылку, которую хотите отслеживать";
            case AWAITING_LINK:
                if (isValidUrl(text)) {
                    userStateManager.setLink(chatId, text);
                    userStateManager.setState(chatId, TrackState.AWAITING_TAGS);
                    return "Теперь укажите теги через запятую или отправьте 'пропустить':";
                } else{
                    return "Некорректная ссылка. Попробуйте еще раз";
                }
            case AWAITING_TAGS:
                String link = userStateManager.getLink(chatId);
                List<String> tags = parseTags(text);
                AddLinkRequest request = new AddLinkRequest();
                request.setLink(link);
                request.setTags(tags);
                LinkResponse response = commandExecutor.executeScrapperCall(
                    () -> scrapperClient.addLink(chatId, request),
                    chatId,
                    "Ошибка при добавлении ссылки. Попробуйте позже."
                );
                userStateManager.clear(chatId);
                if (response != null){
                    return "Ссылка успешно добавлена!";
                } else {
                    return null;
                }
            default:
                return null;
        }
    }


    @Override
    public String getCommand() {
        return "/track";
    }

    @Override
    public String getDescription() {
        return "Начать отслеживание ссылки";
    }

    private List<String> parseTags(String input) {
        if (input == null || input.isBlank() || input.equalsIgnoreCase("пропустить")) {
            return List.of();
        }
        return Arrays.stream(input.split(","))
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .collect(Collectors.toList());
    }
}
