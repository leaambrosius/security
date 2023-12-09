package App.Messages;

import Utils.InvalidMessageException;

public class GetChatMessage extends Message {
    private String username;
    private String chatId;
    private String signature;
    private String lastTimestamp;

    public GetChatMessage(String username, String chatId, String signature, String lastTimestamp) {
        super(MessageType.GET_CHAT);
        this.username = username;
        this.chatId = chatId;
        this.signature = signature;
        this.lastTimestamp = lastTimestamp;
    }

    public String encode() {
        return type + "@" + username + "@" + chatId + "@" + signature +"@" + lastTimestamp;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 5 && parts[0].equals(type.toString())) {
            this.username = parts[1];
            this.chatId = parts[2];
            this.signature = parts[3];
            this.lastTimestamp = parts[4];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static GetChatMessage fromString(String message) throws InvalidMessageException {
        GetChatMessage getChatMessage = new GetChatMessage("", "", "", "");
        getChatMessage.decode(message);
        return getChatMessage;
    }

    public String getSignature() {
        return signature;
    }

    public String getUsername() {
        return username;
    }

    public String getChatId() {
        return chatId;
    }

    public String getLastTimestamp() {
        return lastTimestamp;
    }
}
