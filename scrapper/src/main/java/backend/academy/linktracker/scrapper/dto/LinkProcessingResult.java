package backend.academy.linktracker.scrapper.dto;

import java.util.List;

public record LinkProcessingResult(
        Long linkId,
        String url,
        boolean success,
        boolean updatesFound,
        int eventsCount,
        List<Long> subscriberChatIds,
        String errorMessage) {}
