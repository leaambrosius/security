package App.Storage;

import App.Messages.ChatMessage;
import App.Messages.GroupMessage;

public class StorageMessage {
    public String messageId;
    public String timestamp;
    public String sender;
    public String chatId;
    public String message;
    public String signature;

    public StorageMessage(String messageId, String timestamp, String sender, String chatId, String message, String signature) {
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.sender = sender;
        this.chatId = chatId;
        this.message = message;
        this.signature = signature;
    }

    public StorageMessage(ChatMessage message, String sender, String chatId) {
        this.messageId = message.getMessageId();
        this.timestamp = message.getTimestamp();
        this.message = message.getMessageText();
        this.signature = message.getSignature();

        this.sender = sender;
        this.chatId = chatId;

    }

    public StorageMessage(GroupMessage message, String sender) {
        this.messageId = message.getMessageId();
        this.timestamp = message.getTimestamp();
        this.message = message.getMessageText();
        this.signature = message.getSignature();
        this.chatId = message.getGroupName();

        this.sender = sender;
    }
}
