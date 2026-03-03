package backend.academy.linktracker.command;

import backend.academy.linktracker.port.dto.UpdateData;

/**
 * Интерфейс стратегии для обработки команд бота.
 */
public interface BotCommandCreation {
    /**
     * Выполнить команду
     *
     * @param updateData входящее обновление от Telegram
     * @return текст ответа
     */
    String execute(UpdateData updateData);

    /**
     * @return текст команды (например, "/start")
     */
    String getCommand();

    /**
     * @return описание команды для меню
     */
    String getDescription();
}
