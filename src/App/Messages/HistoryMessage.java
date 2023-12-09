package App.Messages;

import Utils.InvalidMessageException;

import java.util.ArrayList;
import java.util.Arrays;

public class HistoryMessage extends Message {
    private String chatId;
    private String serializedData;
    private ArrayList<String> serializedDataList;

    public HistoryMessage(String chatId, String serializedData) {
        super(MessageType.HISTORY);
        this.chatId = chatId;
        this.serializedData = serializedData;
    }

    public String encode() {
        return type + "@" + chatId + "@" + serializedData;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length > 2 && parts[0].equals(type.toString())) {
            this.chatId = parts[1];

            serializedDataList = new ArrayList<>();
            serializedDataList.addAll(Arrays.asList(parts).subList(2, parts.length));
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static HistoryMessage fromString(String message) throws InvalidMessageException {
        HistoryMessage peerDataMessage = new HistoryMessage("", "");
        peerDataMessage.decode(message);
        return peerDataMessage;
    }

    public String getChatId() {
        return chatId;
    }

    public ArrayList<String> getSerializedDataList() {
        return serializedDataList;
    }

    public static String getNACK() {
        return MessageType.HISTORY + "@NACK";
    }
}
