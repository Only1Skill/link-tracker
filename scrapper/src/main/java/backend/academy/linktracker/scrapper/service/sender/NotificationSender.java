package backend.academy.linktracker.scrapper.service.sender;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import java.util.List;

public interface NotificationSender {
    void sendUpdates(List<LinkUpdate> updates);
}
