package backend.academy.linktracker.handler;

public interface CommandHandler {
    String handleStart();
    String handleHelp();
    String handleUnknown();
}
