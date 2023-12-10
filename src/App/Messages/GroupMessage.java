package App.Messages;

import Utils.InvalidMessageException;

import java.security.SecureRandom;
import java.util.Base64;

// GROUP_MESSAGE @ MESSAGE_ID @ TIMESTAMP @ SIGNATURE @ MESSAGE
public class GroupMessage extends Message {
    private String messageId;
    private String timestamp;
    private String signature;
    private String messageText;
    private String groupName;

    public GroupMessage(String messageId, String timestamp, String signature, String messageText, String groupName) {
        super(MessageType.GROUP_MESSAGE);
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.messageText = messageText;
        this.signature = signature;
        this.groupName = groupName;
    }

    public GroupMessage(String signature, String messageText, String groupName) {
        super(MessageType.GROUP_MESSAGE);
        this.timestamp = String.valueOf(System.currentTimeMillis());
        this.messageText = messageText;
        this.messageId = generateMessageId();
        this.signature = signature;
        this.groupName = groupName;
    }

    public String encode() {
        return type + "@" + groupName + "@" + messageId + "@" + timestamp + "@" + signature + "@" + messageText;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 6 && parts[0].equals(type.toString())) {
            this.groupName = parts[1];
            this.messageId = parts[2];
            this.timestamp = parts[3];
            this.signature = parts[4];
            this.messageText = parts[5];

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

    public static GroupMessage fromString(String message) throws InvalidMessageException {
        GroupMessage groupMessage = new GroupMessage("", "", "", "", "");
        groupMessage.decode(message);
        return groupMessage;
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

    public String getGroupName() {
        return groupName;
    }
}

