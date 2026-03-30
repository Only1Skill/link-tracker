package backend.academy.linktracker.exception;

public class ScrapperClientException extends RuntimeException {
    public ScrapperClientException(String message) {
        super(message);
    }

    public ScrapperClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
