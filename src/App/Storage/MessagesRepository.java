package App.Storage;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.util.*;

public class MessagesRepository {

    private static final int KEY_SIZE = 2048;
    private static final String ALGORITHM = "RSA";
    private static final String SYMMETRIC_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    HashMap<String, ArrayList<Message>> chatRooms = new HashMap<>();


    public void deleteChatrooms() {
        chatRooms.clear();
    }
    public void addChatRooms(ArrayList<String> chats){
        for (String chat : chats) {
            chatRooms.put(chat, new ArrayList<>());
        }
    }

    public void addMessage(Message message) {
        String peer = message.peerUsername;

        if (chatRooms.containsKey(peer)) {
            ArrayList<Message> chatRoomHistory = chatRooms.get(peer);
            chatRoomHistory.add(message);
        }
        System.out.println("chatRooms:" + chatRooms);
    }

    public ArrayList<Message> getChatHistory(String peer) {
        if (chatRooms.containsKey(peer)) {
            System.out.println(chatRooms.get(peer));
            return chatRooms.get(peer);
        } else {
            return new ArrayList<>();
        }
    }

    public void saveAndEncryptRepository() throws NoSuchAlgorithmException, IOException, ClassNotFoundException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Set<String> keys = chatRooms.keySet();

        for (String username : keys) {
            KeyPair keyPair = KeyRepository.getKeys(username);
            byte[] encryptedData = encryptChatRooms(chatRooms, keyPair.getPublic());
            saveEncryptedDataToFile(encryptedData, "encrypted_" + username + "_chatRooms.txt");
        }
    }

    private static byte[] encryptChatRooms(HashMap<String, ArrayList<Message>> chatRooms, PublicKey publicKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException {

        byte[] serializedData;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(chatRooms);
            serializedData = baos.toByteArray();
        }

        KeyGenerator keyGen = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
        SecretKey secretKey = keyGen.generateKey();

        Cipher aesCipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedData = aesCipher.doFinal(serializedData);

        Cipher rsaCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedSymmetricKey = rsaCipher.doFinal(secretKey.getEncoded());

        // Combination of encrypted symmetric key and encrypted data into single byte array
        ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        resultStream.write(encryptedSymmetricKey);
        resultStream.write(encryptedData);

        return resultStream.toByteArray();
    }

    public void decryptChatRooms(String username)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {

        byte[] encryptedData = loadEncryptedDataFromFile("encrypted_"+username+"_chatRooms.txt");

        PrivateKey privateKey = KeyRepository.getKeys(username).getPrivate();


        int keySize = KEY_SIZE / 8;
        byte[] encryptedSymmetricKey = Arrays.copyOfRange(encryptedData, 0, keySize);
        byte[] encryptedActualData = Arrays.copyOfRange(encryptedData, keySize, encryptedData.length);

        Cipher rsaCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedSymmetricKey = rsaCipher.doFinal(encryptedSymmetricKey);

        SecretKeySpec secretKeySpec = new SecretKeySpec(decryptedSymmetricKey, SYMMETRIC_ALGORITHM);
        Cipher aesCipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
        aesCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] decryptedData = aesCipher.doFinal(encryptedActualData);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(decryptedData);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            chatRooms = (HashMap<String, ArrayList<Message>>) ois.readObject();
        }
    }


    private static byte[] loadEncryptedDataFromFile(String fileName) throws IOException {
        try (FileInputStream fis = new FileInputStream(fileName)) {
            return fis.readAllBytes();
        }
    }

    private static void saveEncryptedDataToFile(byte[] encryptedData, String fileName) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(encryptedData);
        }
    }

}
