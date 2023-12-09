package App.Messages;

import Utils.InvalidMessageException;

import java.util.ArrayList;
import java.util.Arrays;

public class SearchChatMessage {
    private String username;
    private String chatId;
    private String encryptedKeyword;

    public SearchChatMessage(String username, String chatId, String encryptedKeyword) {
        this.username = username;
        this.chatId = chatId;
        this.encryptedKeyword = encryptedKeyword;
    }

    public String encode() {
        return username + "@" + chatId + "@" + encryptedKeyword;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 3 && parts[0].equals(username.toString())) {
            this.chatId = parts[1];
            this.encryptedKeyword = parts[2];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }
}
