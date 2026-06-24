package backend.academy.linktracker.scrapper.repository;

public interface TagStorage {
    Long findOrCreate(String name);
}
