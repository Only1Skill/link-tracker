package backend.academy.linktracker.client;

import backend.academy.linktracker.dto.AddLinkRequest;
import backend.academy.linktracker.dto.LinkResponse;
import java.util.List;

public interface ScrapperClient {
    void registerChat(long chatId);
    void deleteChat(long chatId);
    LinkResponse addLink(long chatId, AddLinkRequest request);
    List<LinkResponse> getLinks(long chatId, String tag);
    void removeLink(long chatId, String url);
}
