package App.Storage;

public class ChatRecord {
    public final String peer;
    public final String chatId;
    public final String symmetricKey;

    public ChatRecord(String peer, String chatId, String symmetricKey) {
        this.peer = peer;
        this.chatId = chatId;
        this.symmetricKey = symmetricKey;
    }

    public String toStorageString() {
        StringBuilder chatRecord = new StringBuilder(chatId + "@");
        chatRecord.append(peer + "@");
        chatRecord.append(symmetricKey);
        return String.valueOf(chatRecord);
    }
}
