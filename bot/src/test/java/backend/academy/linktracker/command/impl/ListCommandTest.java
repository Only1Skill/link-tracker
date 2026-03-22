package backend.academy.linktracker.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.dto.LinkResponse;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.service.CommandExecutor;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListCommandTest {
    @Mock
    CommandExecutor commandExecutor;

    @InjectMocks
    ListCommand listCommand;

    @Test
    void whenNoLinks_returnsEmptyMessage() {
        UpdateData update = new UpdateData(1, 123L, "/list", null, null);
        when(commandExecutor.executeScrapperCall(any(), eq(123L))).thenReturn(List.of());
        String result = listCommand.execute(update);
        assertThat(result).isEqualTo("У вас нет отслеживаемых ссылок.");
    }

    @Test
    void whenLinksExist_returnsFormattedList() {
        UpdateData update = new UpdateData(1, 123L, "/list", null, null);
        List<LinkResponse> links = List.of(
                new LinkResponse(1L, URI.create("url1"), List.of("tag1"), null),
                new LinkResponse(2L, URI.create("url2"), null, null));
        when(commandExecutor.executeScrapperCall(any(), eq(123L))).thenReturn(links);
        String result = listCommand.execute(update);
        assertThat(result)
                .contains("Ваши отслеживаемые ссылки:")
                .contains("1. url1 (теги: tag1)")
                .contains("2. url2");
    }
}
