package backend.academy.linktracker.dto;

public record UpdateData(Integer updateId, Long chatId, String messageText, Long userId, String userName) {}
