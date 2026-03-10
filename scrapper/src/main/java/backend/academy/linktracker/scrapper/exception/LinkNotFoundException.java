package backend.academy.linktracker.scrapper.exception;

public class LinkNotFoundException extends ScrapperException {
    public LinkNotFoundException(String url) {
        super("Ссылка " + url + " не найдена");
    }
}
