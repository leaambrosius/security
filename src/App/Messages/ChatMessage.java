package App.Messages;

import App.Client.EncryptionManager;
import Utils.InvalidMessageException;

import java.security.SecureRandom;
import java.util.Base64;

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
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
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
