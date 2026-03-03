package backend.academy.linktracker.command.impl;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.port.dto.UpdateData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StartCommand Unit Tests")
class StartCommandTest {

    private StartCommand startCommand;

    @BeforeEach
    void setUp() {
        startCommand = new StartCommand();
    }

    @Nested
    @DisplayName("execute Method Tests")
    class ExecuteTests {

        @Test
        @DisplayName("Should return welcome message")
        void shouldReturnWelcomeMessage() {
            // given
            UpdateData updateData = new UpdateData(1, 12345L, "/start", 67890L, "testuser");

            // when
            String result = startCommand.execute(updateData);

            // then
            assertThat(result)
                    .contains("Добро пожаловать")
                    .contains("отслеживания изменений")
                    .contains("/help");
        }

        @Test
        @DisplayName("Should handle UpdateData with null fields")
        void shouldHandleUpdateDataWithNullFields() {
            // given
            UpdateData updateData = new UpdateData(null, null, null, null, null);

            // when
            String result = startCommand.execute(updateData);

            // then
            assertThat(result).isNotNull().contains("Добро пожаловать");
        }

        @Test
        @DisplayName("Should return consistent response regardless of input")
        void shouldReturnConsistentResponseRegardlessOfInput() {
            // given
            UpdateData updateData1 = new UpdateData(1, 123L, "/start", 456L, "user1");
            UpdateData updateData2 = new UpdateData(2, 789L, "/start", 101L, "user2");

            // when
            String result1 = startCommand.execute(updateData1);
            String result2 = startCommand.execute(updateData2);

            // then
            assertThat(result1).isEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("Command Metadata Tests")
    class CommandMetadataTests {

        @Test
        @DisplayName("Should return correct command string")
        void shouldReturnCorrectCommandString() {
            // when
            String command = startCommand.getCommand();

            // then
            assertThat(command).isEqualTo("/start");
        }

        @Test
        @DisplayName("Should return correct description")
        void shouldReturnCorrectDescription() {
            // when
            String description = startCommand.getDescription();

            // then
            assertThat(description).isEqualTo("Начало работы с ботом");
        }
    }
}
