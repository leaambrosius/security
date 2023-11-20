package App.Messages;

import java.util.Objects;

public class Message {
    private MessageType type;
    private String[] parts;

    public Message(MessageType type, String[] parts) {
        this.type = type;
        this.parts = parts;
    }

    public MessageType getType() {
        return type;
    }

    public String[] getParts() {
        return parts;
    }

    public boolean isAck() {
        return this.parts.length >= 2 && Objects.equals(this.parts[1], "ACK");
    }

    public boolean verifyLength(int expectedLength) {
        return this.parts.length == expectedLength;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type);
        for (String part : parts) {
            stringBuilder.append("@").append(part);
        }
        return stringBuilder.toString();
    }
}

