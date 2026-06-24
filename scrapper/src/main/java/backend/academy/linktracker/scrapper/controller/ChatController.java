package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.service.ChatApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tg-chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    private final ChatApplicationService chatApplicationService;

    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void registerChat(@PathVariable Long id) {
        chatApplicationService.register(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteChat(@PathVariable Long id) {
        chatApplicationService.delete(id);
    }
}
