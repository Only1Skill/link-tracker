package backend.academy.linktracker.command;

import com.pengrad.telegrambot.model.Update;

/**
 * Интерфейс стратегии для обработки команд бота.
 */
public interface BotCommand {
    /**
     * Выполнить команду
     *
     * @param update входящее обновление от Telegram
     * @return текст ответа
     */
    String execute(Update update);

    /**
     * @return текст команды (например, "/start")
     */
    String getCommand();

    /**
     * @return описание команды для меню
     */
    String getDescription();
}
