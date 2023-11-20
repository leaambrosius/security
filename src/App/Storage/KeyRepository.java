package App.Storage;

import java.io.*;
import java.security.*;

public class KeyRepository {

    private static final String PRIVATE_KEY_FILE = "private_key";
    private static final String PUBLIC_KEY_FILE = "public_key";
    private static final int KEY_SIZE = 2048;

    public static KeyPair createKeys(String username) throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(KEY_SIZE);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        saveKeyToFile(keyPair.getPrivate(), PRIVATE_KEY_FILE + username + ".pem");
        saveKeyToFile(keyPair.getPublic(), PUBLIC_KEY_FILE + username + ".pem");

        return keyPair;
    }

    public static KeyPair getKeys(String username) throws NoSuchAlgorithmException, IOException, ClassNotFoundException {
        KeyPair keyPair;

        try {
            PrivateKey privateKey = (PrivateKey) readKeyFromFile(PRIVATE_KEY_FILE + username + ".pem");
            PublicKey publicKey = (PublicKey) readKeyFromFile(PUBLIC_KEY_FILE + username + ".pem");
            keyPair = new KeyPair(publicKey, privateKey);

        } catch (FileNotFoundException e) {
            keyPair = createKeys(username);
        }

        return keyPair;
    }

    private static void saveKeyToFile(Key key, String fileName) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(key);
        }
    }

    private static Key readKeyFromFile(String fileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            return (Key) ois.readObject();
        }
    }

    public static boolean keysExist(String username) {
        File privateKeyFile = new File(PRIVATE_KEY_FILE + username + ".pem");
        File publicKeyFile = new File(PUBLIC_KEY_FILE + username + ".pem");
        return privateKeyFile.exists() && publicKeyFile.exists();
    }
}