package App.Storage;

import App.Client.EncryptionManager;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

public class FileManager {
    static final String CONTACTS_FILENAME = "contacts.txt";
    static final String GROUPS_FILENAME = "groups.txt";

    static String username;

    public static void setUsername(String username) {
        FileManager.username = username;
    }

    public static ArrayList<ChatRecord> readContacts() throws RuntimeException {
        ArrayList<ChatRecord> chats = new ArrayList<>();
        if (!Files.exists(Path.of(CONTACTS_FILENAME))) return chats;

        try {
            try (BufferedReader br = new BufferedReader(new FileReader(CONTACTS_FILENAME))) {
                String line;
                while ((line = br.readLine()) != null) {
                    try {
                        String plainText = decrypt(line);
                        String[] parts = plainText.split("@");
                        if (parts.length == 3) {
                            String chatId = parts[0];
                            String peer = parts[1];
                            String symmetricKey = parts[2];
                            chats.add(new ChatRecord(peer, chatId, symmetricKey));
                        }
                        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                            BadPaddingException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (NoSuchAlgorithmException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return chats;
    }

    public static ArrayList<GroupRecord> readGroupContacts() {
        ArrayList<GroupRecord> chats = new ArrayList<>();


        if (!Files.exists(Path.of(GROUPS_FILENAME))) return chats;

        try (BufferedReader br = new BufferedReader(new FileReader(GROUPS_FILENAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String plainText = decrypt(line);
                String[] parts = plainText.split("@");
                if (parts.length > 2) {
                    String groupName = parts[0];
                    String symmetricKey = parts[1];
                    ArrayList<String> members = new ArrayList<>(Arrays.asList(parts).subList(2, parts.length));
                    chats.add(new GroupRecord(members, groupName, symmetricKey));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return chats;
    }

    public static void saveContact(ChatRecord chat) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONTACTS_FILENAME, true))) {
            File file = new File(CONTACTS_FILENAME);

            if (file.exists() && file.length() > 0) {
                writer.newLine();
            }
            try {
               String cypherText = encrypt(chat.toStorageString());
               writer.write(cypherText);
            } catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException |
                     NoSuchPaddingException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveGroup(GroupRecord group) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(GROUPS_FILENAME, true))) {
            File file = new File(GROUPS_FILENAME);
            if (file.exists() && file.length() > 0) {
                writer.newLine();
            }
            String cypherText = encrypt(group.toStorageString());
            writer.write(cypherText);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encrypt(String message) throws NoSuchAlgorithmException, IOException, ClassNotFoundException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        KeyPair keyPair = KeyRepository.getKeys(FileManager.username);
        EncryptionManager encryptionManager = new EncryptionManager(keyPair);
        byte[] cypherText = encryptionManager.encryptWithOwnPublicKey(message);
        return Base64.getEncoder().encodeToString(cypherText);
    }

    private static String decrypt(String message) throws NoSuchAlgorithmException, IOException, ClassNotFoundException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        KeyPair keyPair = KeyRepository.getKeys(FileManager.username);
        EncryptionManager encryptionManager = new EncryptionManager(keyPair);
        return encryptionManager.decryptWithPrivateKey(message);
    }
}
