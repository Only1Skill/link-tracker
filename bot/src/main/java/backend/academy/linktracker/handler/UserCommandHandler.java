package backend.academy.linktracker.handler;

import org.springframework.stereotype.Component;

@Component
public class UserCommandHandler implements CommandHandler{
    @Override
    public String handleStart() {
        return """
            Добро пожаловать! Я бот для отслеживания изменений на сайтах.

            Я буду уведомлять вас, когда на интересующих вас страницах появятся изменения.

            Используйте /help для списка доступных команд.
            """;
    }

    @Override
    public String handleHelp() {
        return """
            Доступные команды:

            /start - Начало работы
            /help - Показать это сообщение
            """;
    }

    @Override
    public String handleUnknown() {
        return "Извините, я не понимаю эту команду. Используйте /help для списка команд.";
    }
}
