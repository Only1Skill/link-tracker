package backend.academy.linktracker.service;

import static org.mockito.Mockito.*;

import backend.academy.linktracker.command.CommandRegistry;
import backend.academy.linktracker.command.impl.HelpCommand;
import backend.academy.linktracker.command.impl.StartCommand;
import backend.academy.linktracker.command.impl.UnknownCommand;
import backend.academy.linktracker.port.TelegramClient;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        classes = {
            TelegramBotService.class,
            CommandRegistry.class,
            StartCommand.class,
            HelpCommand.class,
            UnknownCommand.class
        })
@TestPropertySource(properties = "app.telegram.token=fake-token")
public class TelegramBotServiceSimpleTest {

    @MockitoBean
    private TelegramClient telegramClient;

    @Autowired
    private TelegramBotService telegramBotService;

    @Test
    void testProcessUpdateWithStartCommand() {
        // Создаём мок входящего обновления
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        User user = mock(User.class);

        when(update.message()).thenReturn(message);
        when(message.text()).thenReturn("/start");
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        when(message.from()).thenReturn(user);
        when(user.id()).thenReturn(456L);

        // Вызываем тестируемый метод
        telegramBotService.handle(update);

        // Проверяем, что бот отправил ответ
        verify(telegramClient, times(1)).sendMessage(eq(123L), anyString());
    }

    @Test
    void testProcessUpdateWithUnknownCommand() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        User user = mock(User.class);

        when(update.message()).thenReturn(message);
        when(message.text()).thenReturn("/unknown");
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        when(message.from()).thenReturn(user);
        when(user.id()).thenReturn(456L);

        telegramBotService.handle(update);

        verify(telegramClient, times(1)).sendMessage(eq(123L), anyString());
    }
}
