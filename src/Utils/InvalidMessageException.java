package Utils;

public class InvalidMessageException extends Exception {
    public InvalidMessageException(String message) {
        super("Received invalid message: " + message);
    }
}