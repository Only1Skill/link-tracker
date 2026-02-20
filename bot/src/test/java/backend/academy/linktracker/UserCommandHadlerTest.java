package backend.academy.linktracker;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.handler.UserCommandHandler;
import org.junit.jupiter.api.Test;

class UserCommandHandlerTest {

    private final UserCommandHandler handler = new UserCommandHandler();

    @Test
    void handleStart_ShouldReturnWelcomeMessage() {
        String result = handler.handleStart();

        assertThat(result)
                .isNotEmpty()
                .contains("Добро пожаловать")
                .contains("/help")
                .contains("отслеживания изменений на сайтах");
    }

    @Test
    void handleHelp_ShouldReturnCommandsList() {
        String result = handler.handleHelp();

        assertThat(result)
                .isNotEmpty()
                .contains("/start")
                .contains("/help")
                .contains("Начало работы")
                .contains("Показать это сообщение");
    }

    @Test
    void handleUnknown_ShouldReturnErrorMessage() {
        String result = handler.handleUnknown();

        assertThat(result).isNotEmpty().contains("не понимаю").contains("/help");
    }
}
