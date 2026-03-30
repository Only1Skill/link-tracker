package backend.academy.linktracker.scrapper.exception;

public abstract class ScrapperException extends RuntimeException {
    public ScrapperException(String message) {
        super(message);
    }
}
