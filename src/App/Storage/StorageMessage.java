package App.Storage;

import App.Client.EncryptionManager;
import App.Messages.ChatMessage;
import App.Messages.GroupMessage;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class StorageMessage implements Serializable {
    public String messageId;
    public String timestamp;
    public String sender;
    public String chatId;
    public String message;
    public String signature;

    public StorageMessage(String messageId, String timestamp, String encryptedSender, String chatId, String encryptedMessage, String signature) {
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.chatId = chatId;
        this.signature = signature;
        this.sender = encryptedSender;
        this.message = encryptedMessage;
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

    public StorageMessage encrypted(EncryptionManager em, SecretKey storageKey) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String encMessage = em.encryptWithSymmetricKey(this.message, storageKey);
        String encryptedSender = em.encryptWithSymmetricKey(this.sender, storageKey);
        return new StorageMessage(messageId, timestamp, encryptedSender, chatId, encMessage, signature);
    }

    public void decrypt(EncryptionManager em, SecretKey storageKey) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.message = em.decryptWithSymmetricKey(this.message, storageKey);
        this.sender = em.decryptWithSymmetricKey(this.sender, storageKey);
    }

    public String serialize() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(this);
        objectOutputStream.close();
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }

    public static StorageMessage deserialize(String serializedData) throws IOException, ClassNotFoundException {
        byte[] byteArray = Base64.getDecoder().decode(serializedData);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        Object object = objectInputStream.readObject();
        objectInputStream.close();

        if (object instanceof StorageMessage) {
            return (StorageMessage) object;
        }
        return null;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
