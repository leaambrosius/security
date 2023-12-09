package App.Storage;

import App.Client.Peer;
import App.UI.GroupChatViewUI;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.io.*;
import java.rmi.Remote;
import java.security.*;
import java.util.*;

public class MessagesRepository {
    private static final int KEY_SIZE = 2048;
    private static final String SYMMETRIC_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    // Chat ID -> Messages
    public HashMap<String, ArrayList<StorageMessage>> chatsHistory = new HashMap<>();
    public HashMap<String, ChatRecord> chats = new HashMap<>();
    public HashMap<String, ChatRecord> peerToChat = new HashMap<>();
    public HashMap<String, GroupRecord> groups = new HashMap<>();

    private static MessagesRepository messagesRepository; // Singleton instance

    public static MessagesRepository mr() {
        if (messagesRepository == null) {
            messagesRepository = new MessagesRepository();
        }
        return messagesRepository;
    }

    public MessagesRepository() {
        ArrayList<ChatRecord> chatRecords = FileManager.readContacts();
        for (ChatRecord r : chatRecords) {
            chats.put(r.chatId, r);
            peerToChat.put(r.peer, r);
            chatsHistory.put(r.chatId, new ArrayList<>());
        }

        ArrayList<GroupRecord> groupRecords = FileManager.readGroupContacts();
        for (GroupRecord r : groupRecords) {
            groups.put(r.chatId, r);
            chatsHistory.put(r.chatId, new ArrayList<>());
        }
    }

    public void addMessage(StorageMessage message) {
        if (chatsHistory.containsKey(message.chatId)) {
            chatsHistory.get(message.chatId).add(message);
        }
    }

    public void addGroup(GroupRecord group) {
        if (!groups.containsKey(group.chatId)) {
            groups.put(group.chatId, group);
            chatsHistory.put(group.chatId, new ArrayList<>());
            FileManager.saveGroup(group);
        }
    }

    public void addChat(ChatRecord chat, Peer host) {
        if (!chats.containsKey(chat.chatId)) {
            chats.put(chat.chatId, chat);
            peerToChat.put(chat.peer, chat);
            chatsHistory.put(chat.chatId, new ArrayList<>());
            FileManager.saveContact(chat);
            host.registerChatToRemote(chat.chatId);
        }
    }

    public ArrayList<StorageMessage> getChatHistory(String chatId) {
        return chatsHistory.getOrDefault(chatId, new ArrayList<>());
    }

    public String getStorageKey(String name) {
        if (peerToChat.containsKey(name)) {
            return peerToChat.get(name).symmetricKey;
        } else if (groups.containsKey(name)) {
            return groups.get(name).symmetricKey;
        }
        return "";
    }

    public String getStorageKeyByChat(String chatId) {
        if (chats.containsKey(chatId)) {
            return chats.get(chatId).symmetricKey;
        } else if (groups.containsKey(chatId)) {
            return groups.get(chatId).symmetricKey;
        }
        return "";
    }

    public String getChatId(String name) {
        if (peerToChat.containsKey(name)) {
            return peerToChat.get(name).chatId;
        }
        return null;
    }

    public void encryptAndSendToStorage() {
//        Set<String> chats = chatRooms.keySet();
        // TODO
    }

//    private static byte[] encryptChatRooms(HashMap<String, ArrayList<Message>> chatRooms, PublicKey publicKey)
//            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException {
//
//        byte[] serializedData;
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
//             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
//
//            oos.writeObject(chatRooms);
//            serializedData = baos.toByteArray();
//        }
//
//        KeyGenerator keyGen = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
//        SecretKey secretKey = keyGen.generateKey();
//
//        Cipher aesCipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
//        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
//        byte[] encryptedData = aesCipher.doFinal(serializedData);
//
//        Cipher rsaCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
//        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
//        byte[] encryptedSymmetricKey = rsaCipher.doFinal(secretKey.getEncoded());
//
//        // Combination of encrypted symmetric key and encrypted data into single byte array
//        ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
//        resultStream.write(encryptedSymmetricKey);
//        resultStream.write(encryptedData);
//
//        return resultStream.toByteArray();
//    }

//    public void decryptFromStorage(String username)
//            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
//
//        byte[] encryptedData = loadEncryptedDataFromFile("encrypted_"+username+"_chatRooms.txt");
//
//        PrivateKey privateKey = KeyRepository.getKeys(username).getPrivate();
//
//
//        int keySize = KEY_SIZE / 8;
//        byte[] encryptedSymmetricKey = Arrays.copyOfRange(encryptedData, 0, keySize);
//        byte[] encryptedActualData = Arrays.copyOfRange(encryptedData, keySize, encryptedData.length);
//
//        Cipher rsaCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
//        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
//        byte[] decryptedSymmetricKey = rsaCipher.doFinal(encryptedSymmetricKey);
//
//        SecretKeySpec secretKeySpec = new SecretKeySpec(decryptedSymmetricKey, SYMMETRIC_ALGORITHM);
//        Cipher aesCipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
//        aesCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
//        byte[] decryptedData = aesCipher.doFinal(encryptedActualData);
//
//        try (ByteArrayInputStream bais = new ByteArrayInputStream(decryptedData);
//             ObjectInputStream ois = new ObjectInputStream(bais)) {
//            chatRooms = (HashMap<String, ArrayList<Message>>) ois.readObject();
//        }
//    }
}
