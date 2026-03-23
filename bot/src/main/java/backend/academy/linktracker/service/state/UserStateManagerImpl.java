package backend.academy.linktracker.service.state;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class UserStateManagerImpl implements UserStateManager {
    private final ConcurrentHashMap<Long, UserState> states = new ConcurrentHashMap<>();

    public UserState getOrCreate(Long chatId) {
        return states.computeIfAbsent(chatId, k -> new UserState());
    }

    public void setState(Long chatId, TrackState state) {
        getOrCreate(chatId).setState(state);
    }

    public TrackState getState(Long chatId) {
        return states.getOrDefault(chatId, new UserState()).getState();
    }

    public void setLink(Long chatId, String link) {
        getOrCreate(chatId).setLink(link);
    }

    public String getLink(Long chatId) {
        return states.getOrDefault(chatId, new UserState()).getLink();
    }

    public void clear(Long chatId) {
        states.remove(chatId);
    }
}
