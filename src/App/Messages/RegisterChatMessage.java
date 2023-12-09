package App.Messages;

import Utils.InvalidMessageException;

public class RegisterChatMessage extends Message {
    private String username;
    private String chatId;
    private String signature;

    public RegisterChatMessage(String username, String chatId, String signature) {
        super(MessageType.REGISTER_CHAT);
        this.username = username;
        this.chatId = chatId;
        this.signature = signature;
    }

    public String encode() {
        return type + "@" + username + "@" + chatId + "@" + signature;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 4 && parts[0].equals(type.toString())) {
            this.username = parts[1];
            this.chatId = parts[2];
            this.signature = parts[3];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static RegisterChatMessage fromString(String message) throws InvalidMessageException {
        RegisterChatMessage registerMessage = new RegisterChatMessage("", "", "");
        registerMessage.decode(message);
        return registerMessage;
    }

    public String getUsername() {
        return username;
    }
    public String getChatId() { return chatId; }
    public String getSignature() { return signature; }

    public static String getNACK() {
        return MessageType.REGISTER_CHAT + "@NACK";
    }
}
