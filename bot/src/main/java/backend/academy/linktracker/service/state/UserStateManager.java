package backend.academy.linktracker.service.state;

public interface UserStateManager {
    UserState getOrCreate(Long chatId);

    void setState(Long chatId, TrackState state);

    TrackState getState(Long chatId);

    void setLink(Long chatId, String link);

    String getLink(Long chatId);

    void clear(Long chatId);
}
