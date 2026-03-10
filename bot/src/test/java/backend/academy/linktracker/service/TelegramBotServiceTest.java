package backend.academy.linktracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.client.ScrapperClient;
import backend.academy.linktracker.client.TelegramClient;
import backend.academy.linktracker.command.BotCommandCreation;
import backend.academy.linktracker.command.impl.CommandRegistryImpl;
import backend.academy.linktracker.command.impl.HelpCommand;
import backend.academy.linktracker.command.impl.StartCommand;
import backend.academy.linktracker.dto.UpdateData;
import backend.academy.linktracker.service.state.UserStateManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TelegramBotServiceTest.TestConfig.class)
@DisplayName("Spring-компонентные тесты TelegramBotService")
class TelegramBotServiceTest {

    @Configuration
    @Import({TelegramBotService.class, CommandRegistryImpl.class})
    static class TestConfig {

        @Bean
        public TelegramClient telegramClient() {
            return Mockito.mock(TelegramClient.class);
        }

        @Bean
        public ScrapperClient scrapperClient() {
            return Mockito.mock(ScrapperClient.class);
        }

        @Bean
        public CommandExecutor commandExecutor(TelegramClient telegramClient) {
            return new CommandExecutor(telegramClient);
        }

        @Bean
        public UserStateManager userStateManager() {
            return new UserStateManager();
        }

        @Bean
        public BotCommandCreation startCommand(ScrapperClient scrapperClient, CommandExecutor commandExecutor) {
            return new StartCommand(scrapperClient, commandExecutor);
        }

        @Bean
        public BotCommandCreation helpCommand(@Lazy CommandRegistryImpl commandRegistry) {
            return new HelpCommand(commandRegistry);
        }
    }

    @Autowired
    private TelegramBotService telegramBotService;

    @Autowired
    private TelegramClient telegramClient;

    private ArgumentCaptor<String> messageCaptor;
    private ArgumentCaptor<Long> chatIdCaptor;

    @BeforeEach
    void setUp() {
        messageCaptor = ArgumentCaptor.forClass(String.class);
        chatIdCaptor = ArgumentCaptor.forClass(Long.class);
    }

    @AfterEach
    void tearDown() {
        reset(telegramClient);
    }

    @Test
    @DisplayName("Должен обработать /start команду")
    void shouldHandleStartCommand() {
        // given
        UpdateData updateData = new UpdateData(1, 12345L, "/start", 67890L, "testuser");

        // when
        telegramBotService.handle(updateData);

        // then
        verify(telegramClient).sendMessage(chatIdCaptor.capture(), messageCaptor.capture());
        assertThat(chatIdCaptor.getValue()).isEqualTo(12345L);
        assertThat(messageCaptor.getValue()).contains("Добро пожаловать").contains("/help");
    }

    @Test
    @DisplayName("Должен обработать /help команду")
    void shouldHandleHelpCommand() {
        // given
        UpdateData updateData = new UpdateData(1, 12345L, "/help", 67890L, "testuser");

        // when
        telegramBotService.handle(updateData);

        // then
        verify(telegramClient).sendMessage(chatIdCaptor.capture(), messageCaptor.capture());
        assertThat(chatIdCaptor.getValue()).isEqualTo(12345L);
        assertThat(messageCaptor.getValue())
                .contains("Доступные команды")
                .contains("/start")
                .contains("/help");
    }

    @Test
    @DisplayName("Должен ответить на неизвестную команду")
    void shouldHandleUnknownCommand() {
        // given
        UpdateData updateData = new UpdateData(1, 12345L, "/unknown", 67890L, "testuser");

        // when
        telegramBotService.handle(updateData);

        // then
        verify(telegramClient).sendMessage(chatIdCaptor.capture(), messageCaptor.capture());
        assertThat(chatIdCaptor.getValue()).isEqualTo(12345L);
        assertThat(messageCaptor.getValue())
                .contains("Извините, я не понимаю эту команду")
                .contains("/help");
    }

    @Test
    @DisplayName("Должен извлечь команду из сообщения с аргументами")
    void shouldExtractCommandFromMessageWithArgs() {
        // given
        UpdateData updateData = new UpdateData(1, 12345L, "/start with arguments", 67890L, "testuser");

        // when
        telegramBotService.handle(updateData);

        // then
        verify(telegramClient).sendMessage(chatIdCaptor.capture(), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("Добро пожаловать");
    }

    @Test
    @DisplayName("Должен обработать команду в любом регистре")
    void shouldHandleCaseInsensitiveCommand() {
        // given
        UpdateData updateData = new UpdateData(1, 12345L, "/START", 67890L, "testuser");

        // when
        telegramBotService.handle(updateData);

        // then
        verify(telegramClient).sendMessage(chatIdCaptor.capture(), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("Добро пожаловать");
    }
}
