package backend.academy.linktracker.scrapper.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class AddLinkRequest{
    private String link;
    private List<String> tags;
}
