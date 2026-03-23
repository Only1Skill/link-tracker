package backend.academy.linktracker.scrapper.exception;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.controller.LinkController;
import backend.academy.linktracker.scrapper.service.LinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LinkController.class)
class GlobalExceptionHandlerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LinkService linkService;

    @Test
    void handleChatNotFound_returnsNotFoundWithApiError() throws Exception {
        when(linkService.getLinks(1L, null)).thenThrow(new ChatNotFoundException(1L));
        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Чат с id 1 не найден"));
    }

    @Test
    void handleLinkDuplicate_returnsBadRequest() throws Exception {
        when(linkService.getLinks(1L, null)).thenThrow(new LinkDuplicateException("url"));
        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ссылка url уже отслеживается"));
    }
}
