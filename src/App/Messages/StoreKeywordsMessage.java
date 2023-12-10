package App.Messages;

import Utils.InvalidMessageException;

import java.util.ArrayList;
import java.util.Arrays;

public class StoreKeywordsMessage extends Message {
    private String username;
    private String chatId;
    private String messageId;
    private String signature;
    private ArrayList<String> serializedDataList;

    public StoreKeywordsMessage(String username, String chatId, String signature, ArrayList<String> serializedData, String messageId) {
        super(MessageType.STORE_KEYWORD);
        this.username = username;
        this.chatId = chatId;
        this.signature = signature;
        this.serializedDataList = serializedData;
        this.messageId = messageId;
    }

    public String encode() {
        return type + "@" + username + "@" + chatId + "@" + signature + "@"+messageId+ "@" + String.join("@", serializedDataList);
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length > 5 && parts[0].equals(type.toString())) {
            this.username = parts[1];
            this.chatId = parts[2];
            this.signature = parts[3];
            this.messageId = parts[4];

            serializedDataList = new ArrayList<>();
            serializedDataList.addAll(Arrays.asList(parts).subList(5, parts.length));
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static StoreKeywordsMessage fromString(String message) throws InvalidMessageException {
        StoreKeywordsMessage storeKeywordsMessage = new StoreKeywordsMessage("", "", "", null,"");
        storeKeywordsMessage.decode(message);
        return storeKeywordsMessage;
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
        return MessageType.STORE_KEYWORD + "@NACK";
    }

    public String getMessageId() {
        return messageId;
    }
}
