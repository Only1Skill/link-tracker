package backend.academy.linktracker.scrapper.repository;

public interface ChatStorage {
    void save(Long chatId);

    boolean exists(Long chatId);

    void delete(Long chatId);
}
