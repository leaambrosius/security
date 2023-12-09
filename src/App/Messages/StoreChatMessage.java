package App.Messages;

import Utils.InvalidMessageException;

import java.util.ArrayList;
import java.util.Arrays;

public class StoreChatMessage extends Message {
    private String username;
    private String chatId;
    private String signature;
    private ArrayList<String> serializedDataList;

    public StoreChatMessage(String username, String chatId, String signature, ArrayList<String> serializedData) {
        super(MessageType.STORE_CHAT);
        this.username = username;
        this.chatId = chatId;
        this.signature = signature;
        this.serializedDataList = serializedData;
    }

    public String encode() {
        return type + "@" + username + "@" + chatId + "@" + signature + "@" + String.join("@", serializedDataList);
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length > 3 && parts[0].equals(type.toString())) {
            this.username = parts[1];
            this.chatId = parts[2];
            this.signature = parts[3];

            serializedDataList = new ArrayList<>();
            serializedDataList.addAll(Arrays.asList(parts).subList(3, parts.length));
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static StoreChatMessage fromString(String message) throws InvalidMessageException {
        StoreChatMessage storeChatMessage = new StoreChatMessage("", "", "", null);
        storeChatMessage.decode(message);
        return storeChatMessage;
    }

    public String getChatId() {
        return chatId;
    }

    public String getUsername() {
        return username;
    }

    public String getSignature() {
        return signature;
    }

    public ArrayList<String> getSerializedDataList() {
        return serializedDataList;
    }

    public static String getNACK() {
        return MessageType.STORE_CHAT + "@NACK";
    }
}
