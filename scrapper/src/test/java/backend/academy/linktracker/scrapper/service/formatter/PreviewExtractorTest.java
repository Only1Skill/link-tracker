package backend.academy.linktracker.scrapper.service.formatter;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.properties.NotificationProperties;
import org.junit.jupiter.api.Test;

class PreviewExtractorTest {

    private final NotificationProperties notificationProperties = new NotificationProperties();
    private final PreviewExtractor previewExtractor = new PreviewExtractor(notificationProperties);

    @Test
    void makePreview_shouldReturnFallback_whenTextIsNull() {
        String preview = previewExtractor.makePreview(null, 200);
        assertThat(preview).isEqualTo("Превью отсутствует");
    }

    @Test
    void makePreview_shouldReturnFallback_whenTextIsBlank() {
        String preview = previewExtractor.makePreview("   \n\t   ", 200);
        assertThat(preview).isEqualTo("Превью отсутствует");
    }

    @Test
    void makePreview_shouldRemoveHtmlAndNormalizeSpaces() {
        String raw = "<p>Hello   <b>world</b></p>\n<div>next line</div>";

        String preview = previewExtractor.makePreview(raw, 200);

        assertThat(preview).isEqualTo("Hello world next line");
    }

    @Test
    void makePreview_shouldTrimToMaxLength() {
        String raw = "a".repeat(250);

        String preview = previewExtractor.makePreview(raw, 200);

        assertThat(preview).hasSize(203);
        assertThat(preview).isEqualTo("a".repeat(200) + "...");
    }

    @Test
    void makePreview_shouldReturnOriginal_whenTextShortEnough() {
        String raw = "Short preview";

        String preview = previewExtractor.makePreview(raw, 200);

        assertThat(preview).isEqualTo("Short preview");
    }
}
