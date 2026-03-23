package backend.academy.linktracker.scrapper.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.repository.ChatStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryChatStorageTest {
    private ChatStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryChatStorage();
    }

    @Test
    void save_and_exists_shouldReturnTrueForSavedChat() {
        storage.save(1L);
        assertThat(storage.exists(1L)).isTrue();
        assertThat(storage.exists(2L)).isFalse();
    }

    @Test
    void delete_shouldRemoveChat() {
        storage.save(1L);
        storage.delete(1L);
        assertThat(storage.exists(1L)).isFalse();
    }
}
