package App.Messages;

import Utils.InvalidMessageException;

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
        return type + "@" + messageId + "@" + timestamp + "@" + signature + "@" + messageText;
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

