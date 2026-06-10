package backend.academy.linktracker.scrapper.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.controller.LinkController;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.service.LinkApplicationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(LinkController.class)
class GlobalExceptionHandlerTest {

    private static final Long CHAT_ID = 123L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LinkApplicationService linkApplicationService;

    @Test
    void handleChatNotFoundException_shouldReturn404() throws Exception {
        AddLinkRequest request = new AddLinkRequest("https://github.com/owner/repo", List.of("java"));

        doThrow(new ChatNotFoundException(CHAT_ID))
                .when(linkApplicationService)
                .addLink(anyLong(), any(AddLinkRequest.class));

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Чат с id 123 не найден"));
    }

    @Test
    void handleLinkDuplicateException_shouldReturn400() throws Exception {
        AddLinkRequest request = new AddLinkRequest("https://github.com/owner/repo", List.of("java"));

        doThrow(new LinkDuplicateException(request.link()))
                .when(linkApplicationService)
                .addLink(anyLong(), any(AddLinkRequest.class));

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ссылка " + request.link() + " уже отслеживается"));
    }

    @Test
    void handleLinkNotFoundException_shouldReturn404() throws Exception {
        AddLinkRequest request = new AddLinkRequest("https://github.com/owner/repo", List.of("java"));

        doThrow(new LinkNotFoundException(request.link()))
                .when(linkApplicationService)
                .removeLink(anyLong(), any());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ссылка " + request.link() + " не найдена"));
    }
}
