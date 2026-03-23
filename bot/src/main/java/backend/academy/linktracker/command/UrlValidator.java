package backend.academy.linktracker.command;

import java.net.URI;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UrlValidator {
    public static boolean isValidUrl(String url) {
        try {
            URI uri = URI.create(url);
            return "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
        } catch (Exception e) {
            return false;
        }
    }
}
