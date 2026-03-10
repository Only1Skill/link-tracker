package backend.academy.linktracker.scrapper.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LogSanitizer {

    /**
     * Удаляет из строки символы CR и LF, заменяя их на пробелы.
     * Если строка null, возвращает null.
     */
    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[\\r\\n]", " ");
    }
}
