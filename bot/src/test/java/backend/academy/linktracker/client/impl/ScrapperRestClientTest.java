package backend.academy.linktracker.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.configuration.ScrapperProperties;
import backend.academy.linktracker.exception.ScrapperClientException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ScrapperRestClientTest {
    @Mock
    private ScrapperProperties properties;

    private ObjectMapper objectMapper;
    private ScrapperRestClient client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        client = new ScrapperRestClient(properties, objectMapper);
    }

    @Test
    void handleError_shouldThrowScrapperClientException_withParsedMessage() throws Exception {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        String errorBody =
                "{\"timestamp\":\"2024-01-01T00:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Chat already exists\",\"path\":\"/tg-chat/123\"}";
        when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(response.getBody()).thenReturn(new ByteArrayInputStream(errorBody.getBytes(StandardCharsets.UTF_8)));

        Method handleError = ScrapperRestClient.class.getDeclaredMethod("handleError", ClientHttpResponse.class);
        handleError.setAccessible(true);

        try {
            handleError.invoke(client, response);
            fail("Expected exception");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertThat(cause).isInstanceOf(ScrapperClientException.class).hasMessage("Chat already exists");
        }
    }

    @Test
    void handleError_shouldThrowScrapperClientException_withDefaultMessage_whenIOException() throws Exception {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(response.getBody()).thenThrow(new IOException("Network failure"));

        Method handleError = ScrapperRestClient.class.getDeclaredMethod("handleError", ClientHttpResponse.class);
        handleError.setAccessible(true);

        try {
            handleError.invoke(client, response);
            fail("Expected exception");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertThat(cause)
                    .isInstanceOf(ScrapperClientException.class)
                    .hasMessageContaining("Не удалось прочитать ответ от сервера");
        }
    }
}
