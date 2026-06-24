package backend.academy.linktracker.scrapper.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkDuplicateException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.service.LinkApplicationService;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(LinkController.class)
class LinkControllerTest {

    private static final Long CHAT_ID = 123L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LinkApplicationService linkApplicationService;

    @Test
    void addLink_shouldReturn200WithResponse() throws Exception {
        AddLinkRequest request = new AddLinkRequest("https://github.com/owner/repo", List.of("java"));
        LinkResponse expected = new LinkResponse(1L, URI.create(request.link()), request.tags(), OffsetDateTime.now());

        when(linkApplicationService.addLink(anyLong(), any(AddLinkRequest.class)))
                .thenReturn(expected);

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.url").value(request.link()))
                .andExpect(jsonPath("$.tags[0]").value("java"));
    }

    @Test
    void addLink_shouldReturn400_whenInvalidRequest() throws Exception {
        AddLinkRequest invalid = new AddLinkRequest("", List.of());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addLink_shouldReturn400_whenLinkDuplicate() throws Exception {
        AddLinkRequest request = new AddLinkRequest("https://github.com/owner/repo", null);

        when(linkApplicationService.addLink(anyLong(), any(AddLinkRequest.class)))
                .thenThrow(new LinkDuplicateException(request.link()));

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ссылка " + request.link() + " уже отслеживается"));
    }

    @Test
    void getLinks_shouldReturn200WithList() throws Exception {
        List<LinkResponse> links = List.of(
                new LinkResponse(
                        1L, URI.create("https://github.com/owner/repo"), List.of("java"), OffsetDateTime.now()),
                new LinkResponse(2L, URI.create("https://stackoverflow.com/questions/1"), null, OffsetDateTime.now()));

        when(linkApplicationService.getLinks(anyLong(), any())).thenReturn(links);

        mockMvc.perform(get("/links").header("Tg-Chat-Id", CHAT_ID).param("tag", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].url").value("https://github.com/owner/repo"))
                .andExpect(jsonPath("$[0].tags[0]").value("java"));
    }

    @Test
    void getLinks_shouldReturn404_whenChatNotFound() throws Exception {
        when(linkApplicationService.getLinks(anyLong(), any())).thenThrow(new ChatNotFoundException(CHAT_ID));

        mockMvc.perform(get("/links").header("Tg-Chat-Id", CHAT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Чат с id 123 не найден"));
    }

    @Test
    void deleteLink_shouldReturn200() throws Exception {
        AddLinkRequest request = new AddLinkRequest("https://github.com/owner/repo", null);

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteLink_shouldReturn404_whenLinkNotFound() throws Exception {
        AddLinkRequest request = new AddLinkRequest("https://github.com/owner/repo", null);

        doThrow(new LinkNotFoundException(request.link()))
                .when(linkApplicationService)
                .removeLink(anyLong(), eq(request.link()));

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ссылка " + request.link() + " не найдена"));
    }
}
