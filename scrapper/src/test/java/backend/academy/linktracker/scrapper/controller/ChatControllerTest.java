package backend.academy.linktracker.scrapper.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatController.class)
class ChatControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatService chatService;

    @Test
    void registerChat_shouldReturn200() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());
    }

    @Test
    void deleteChat_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/tg-chat/1")).andExpect(status().isOk());
    }
}
