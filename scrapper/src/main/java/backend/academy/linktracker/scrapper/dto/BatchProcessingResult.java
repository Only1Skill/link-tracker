package backend.academy.linktracker.scrapper.dto;

import backend.academy.linktracker.scrapper.model.LinkProcessingError;
import java.util.List;

public record BatchProcessingResult(
        int total, int successCount, int failedCount, int updatedLinksCount, List<LinkProcessingError> errors) {}
