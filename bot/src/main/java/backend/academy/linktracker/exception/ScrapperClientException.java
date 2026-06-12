package backend.academy.linktracker.exception;

import lombok.Getter;

@Getter
public class ScrapperClientException extends RuntimeException {

    private final Integer statusCode;

    public ScrapperClientException(String message) {
        super(message);
        this.statusCode = null;
    }

    public ScrapperClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public ScrapperClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

}
