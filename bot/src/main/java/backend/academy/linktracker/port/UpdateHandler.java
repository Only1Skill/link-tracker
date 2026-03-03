package backend.academy.linktracker.port;

import backend.academy.linktracker.port.dto.UpdateData;

@FunctionalInterface
public interface UpdateHandler {
    void handle(UpdateData updateData);
}
