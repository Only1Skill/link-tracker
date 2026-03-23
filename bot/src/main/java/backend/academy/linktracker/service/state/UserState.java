package backend.academy.linktracker.service.state;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class UserState {
    private TrackState state = TrackState.NONE;
    private String link;
}
