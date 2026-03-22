package backend.academy.linktracker.service.state;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserStateManagerImplTest {
    private UserStateManager manager;

    @BeforeEach
    void setUp() {
        manager = new UserStateManagerImpl();
    }

    @Test
    void getOrCreate_createsNewStateIfNotExists() {
        UserState state = manager.getOrCreate(1L);
        assertThat(state).isNotNull();
        assertThat(state.getState()).isEqualTo(TrackState.NONE);
        assertThat(state.getLink()).isNull();
    }

    @Test
    void setState_updatesState() {
        manager.setState(1L, TrackState.AWAITING_LINK);
        assertThat(manager.getState(1L)).isEqualTo(TrackState.AWAITING_LINK);
    }

    @Test
    void setLink_storesLink() {
        manager.setLink(1L, "url");
        assertThat(manager.getLink(1L)).isEqualTo("url");
    }

    @Test
    void clear_removesState() {
        manager.setState(1L, TrackState.AWAITING_LINK);
        manager.clear(1L);
        assertThat(manager.getState(1L)).isEqualTo(TrackState.NONE);
        assertThat(manager.getLink(1L)).isNull();
    }
}
