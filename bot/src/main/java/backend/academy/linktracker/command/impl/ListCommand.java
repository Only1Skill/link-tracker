package backend.academy.linktracker.command.impl;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.dto.LinkResponse;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.service.CommandExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component("/list")
@RequiredArgsConstructor
public class ListCommand implements BotCommandCreation {
    private final ScrapperClient scrapperClient;
    private final CommandExecutor commandExecutor;

    @Override
    public String execute(UpdateData updateData) {
        Long chatId = updateData.chatId();
        String text = updateData.messageText();

        String[] parts = text.split("\\s+", 2);
        String tag = (parts.length > 1) ? parts[1].trim() : null;
        if (tag != null && tag.isBlank()) {
            tag = null;
        }
        String finalTag = tag;

        List<LinkResponse> links = commandExecutor.executeScrapperCall(
            () -> scrapperClient.getLinks(chatId, finalTag),
            chatId,
            "Ошибка при получении списка ссылок. Попробуйте позже."
        );

        if (links == null) {
            return null;
        }

        if (links.isEmpty()) {
            return "У вас нет отслеживаемых ссылок" + (tag == null ? "." : " с тегом '" + tag + "'.");
        }

        String header = tag == null ? "Ваши отслеживаемые ссылки:\n" : "Ваши отслеживаемые ссылки с тегом '" + tag + "':\n";
        String list = IntStream.range(0, links.size())
            .mapToObj(i -> {
                LinkResponse link = links.get(i);
                String tags = (link.getTags() != null && !link.getTags().isEmpty())
                    ? " (теги: " + String.join(", ", link.getTags()) + ")"
                    : "";
                return (i + 1) + ". " + link.getUrl() + tags;
            })
            .collect(Collectors.joining("\n"));

        return header + list;
    }

    @Override
    public String getCommand() {
        return "/list";
    }

    @Override
    public String getDescription() {
        return "Показать список отслеживаемых ссылок (можно указать тег)";
    }
}
