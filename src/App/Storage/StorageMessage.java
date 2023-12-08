package App.Storage;

public class StorageMessage {
    String message_id;
    String timestamp;
    String sender;
    String chat_id;
    String message;
    String signature;

    public StorageMessage(String message_id, String timestamp, String sender, String chat_id, String message, String signature) {
        this.message_id = message_id;
        this.timestamp = timestamp;
        this.sender = sender;
        this.chat_id = chat_id;
        this.message = message;
        this.signature = signature;
    }
}
