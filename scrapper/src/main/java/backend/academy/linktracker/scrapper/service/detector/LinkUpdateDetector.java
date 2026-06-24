package backend.academy.linktracker.scrapper.service.detector;

import backend.academy.linktracker.scrapper.model.LinkEvent;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.util.List;

public interface LinkUpdateDetector {
    boolean supports(String url);

    List<LinkEvent> detectUpdates(TrackedLink trackedLink);
}
