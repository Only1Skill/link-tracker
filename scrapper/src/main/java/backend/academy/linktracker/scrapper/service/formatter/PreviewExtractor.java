package backend.academy.linktracker.scrapper.service.formatter;

import backend.academy.linktracker.scrapper.properties.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PreviewExtractor {

    private static final String EMPTY_PREVIEW = "Превью отсутствует";
    private static final String HTML_TAG_REGEX = "(?s)<[^>]*>";

    private final NotificationProperties notificationProperties;

    public String makePreview(String rawText) {
        return makePreview(rawText, notificationProperties.getPreviewLength());
    }

    public String makePreview(String rawText, int maxLength) {
        if (rawText == null || rawText.isBlank()) {
            return EMPTY_PREVIEW;
        }

        String cleaned = StringEscapeUtils.unescapeHtml4(rawText)
                .replaceAll(HTML_TAG_REGEX, " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isBlank()) {
            return EMPTY_PREVIEW;
        }

        if (cleaned.length() <= maxLength) {
            return cleaned;
        }

        return cleaned.substring(0, maxLength).trim() + "...";
    }
}
