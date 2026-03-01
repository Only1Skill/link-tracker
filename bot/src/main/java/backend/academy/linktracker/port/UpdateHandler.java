package backend.academy.linktracker.port;

import com.pengrad.telegrambot.model.Update;

@FunctionalInterface
public interface UpdateHandler {
    void handle(Update update);
}
