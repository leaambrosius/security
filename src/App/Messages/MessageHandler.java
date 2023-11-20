package App.Messages;

public class MessageHandler {
    public Message decodeMessage(String messageString) {
        String[] parts = messageString.split("@");
        MessageType type = MessageType.valueOf(parts[0]);
        return new Message(type, parts);
    }

    public String encodeMessage(Message message) {
        return message.toString();
    }

    public String generateAck(MessageType type) {
        return type + "@ACK";
    }

    public String generateNack(MessageType type) {
        return type + "@NACK";
    }
}