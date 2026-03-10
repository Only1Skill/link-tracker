package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.service.LinkService;
import backend.academy.linktracker.scrapper.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
@Slf4j
public class LinkController {
    private final LinkService linkService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    public LinkResponse addLink(@RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody @Valid AddLinkRequest request) {
        List<String> safeTags = request.getTags() == null
                ? null
                : request.getTags().stream().map(LogSanitizer::sanitize).collect(Collectors.toList());
        log.info("Add link for chat {}: link={}, tags={}", chatId, LogSanitizer.sanitize(request.getLink()), safeTags);
        return linkService.addLink(chatId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<LinkResponse> getLinks(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestParam(required = false) String tag) {
        return linkService.getLinks(chatId, tag);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public void deleteLink(@RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody @Valid AddLinkRequest request) {
        linkService.removeLink(chatId, request.getLink());
    }
}
