package App.Messages;

import Utils.InvalidMessageException;

import java.util.ArrayList;
import java.util.Arrays;

public class SearchChatMessage extends Message{
    private String username;
    private String signature;
    private String chatId;
    private String encryptedKeyword;

    public SearchChatMessage(String username, String chatId, String encryptedKeyword, String signature) {
        super(MessageType.SEARCH_CHAT);
        this.username = username;
        this.signature = signature;
        this.encryptedKeyword = encryptedKeyword;
        this.chatId = chatId;
    }

    public String encode() {
        return type + "@"+ username + "@" + chatId + "@" + encryptedKeyword + "@" +signature;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 5 && parts[0].equals(type.toString())) {
            this.username = parts[1];
            this.chatId = parts[2];
            this.encryptedKeyword = parts[3];
            this.signature = parts[4];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static SearchChatMessage fromString(String message) throws InvalidMessageException {
        SearchChatMessage searchChatMessage = new SearchChatMessage("", "", "","");
        searchChatMessage.decode(message);
        return searchChatMessage;
    }

    public String getUsername() {
        return username;
    }

    public String getChatId() {
        return chatId;
    }

    public String getSignature() {
        return signature;
    }
    public String getEncryptedKeyword(){return encryptedKeyword;}
}
