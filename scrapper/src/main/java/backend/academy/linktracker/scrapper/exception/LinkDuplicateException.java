package backend.academy.linktracker.scrapper.exception;

public class LinkDuplicateException extends ScrapperException {
    public LinkDuplicateException(String url) {
        super("Ссылка " + url + " уже отслеживается");
    }
}
