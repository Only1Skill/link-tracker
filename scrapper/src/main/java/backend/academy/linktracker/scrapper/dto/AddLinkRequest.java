package backend.academy.linktracker.scrapper.dto;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class AddLinkRequest {
    private String link;
    private List<String> tags;
}
