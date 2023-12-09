package App.Messages;

import Utils.InvalidMessageException;

public class ResponseMessage {
    private final MessageType messageType;
    private final boolean isAck;

    public ResponseMessage(String messageType, boolean isAck) {
        this.messageType = MessageType.valueOf(messageType);
        this.isAck = isAck;
    }

    public boolean isAck() {
        return this.isAck;
    }

    public boolean isType(MessageType type) {
        return this.messageType == type;
    }

    public static ResponseMessage fromString(String message) throws InvalidMessageException {
        if (message == null) throw new InvalidMessageException("Invalid response: null");
        String[] parts = message.split("@");
        if (parts.length == 2 && (parts[1].equals("ACK") || parts[1].equals("NACK"))) {
            return new ResponseMessage(parts[0], parts[1].equals("ACK"));
        } else {
            throw new InvalidMessageException("Invalid response: " + message);
        }
    }

}