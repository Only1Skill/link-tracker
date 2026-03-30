package backend.academy.linktracker.service;

import backend.academy.linktracker.dto.UpdateData;

@FunctionalInterface
public interface UpdateHandler {
    void handle(UpdateData updateData);
}
