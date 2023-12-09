package App.Messages;

import Utils.InvalidMessageException;

// MESSAGE @ MESSAGE_ID @ TIMESTAMP @ SIGNATURE @ MESSAGE
public class ChatMessage extends Message {
    private String messageId;
    private String timestamp;
    private String signature;
    private String messageText;

    public ChatMessage(String messageId, String timestamp, String signature, String messageText) {
        super(MessageType.MESSAGE);
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.messageText = messageText;
        this.signature = signature;
    }

    public ChatMessage(String signature, String messageText) {
        super(MessageType.MESSAGE);
        this.timestamp = String.valueOf(System.currentTimeMillis());
        this.messageText = messageText;
        this.messageId = generateMessageId();
        this.signature = signature;
    }

    public String encode() {
        return type + "@" + messageId + "@" + timestamp + "@" + messageText + "@" + signature;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 5 && parts[0].equals(type.toString())) {
            this.messageId = parts[1];
            this.timestamp = parts[2];
            this.messageText = parts[3];
            this.signature = parts[4];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    private String generateMessageId() {
        String concatenated = messageText + "@" + timestamp;
        return String.valueOf(concatenated.hashCode());
    }

    public static ChatMessage fromString(String message) throws InvalidMessageException {
        ChatMessage chatMessage = new ChatMessage("", "", "", "");
        chatMessage.decode(message);
        return chatMessage;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSignature() {
        return signature;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
