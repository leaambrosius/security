package App.Messages;

import Utils.InvalidMessageException;

import java.util.Objects;

public abstract class Message {
    protected MessageType type;

    public Message(MessageType type) {
        this.type = type;
    }

    public abstract String encode();

    public void decode(String message) throws InvalidMessageException {
    }

    public String generateACK() {
        return type + "@ACK";
    }

    public String generateNACK() {
        return type + "@NACK";
    }

    public MessageType getType() {
        return type;
    }
}

