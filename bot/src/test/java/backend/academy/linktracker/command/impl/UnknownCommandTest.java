package backend.academy.linktracker.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UnknownCommandTest {

    @Mock
    private Update update;

    @Mock
    private Message message;

    @Mock
    private Chat chat;

    private UnknownCommand unknownCommand;

    @BeforeEach
    void setUp() {
        unknownCommand = new UnknownCommand();
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn("/unknownCommand");
    }

    @Test
    void shouldReturnUnknownMessage() {
        String result = unknownCommand.execute(update);
        assertThat(result).isEqualTo("Извините, я не понимаю эту команду. Используйте /help для списка команд.");
    }
}
