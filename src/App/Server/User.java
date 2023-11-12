package App.Server;

import Utils.PublicKeyUtils;

import java.io.*;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.*;
import java.util.Base64;
import java.security.*;

/**Server Side user class**/
public class User {
    private final String username;
    private String ip;
    private String port;
    private PublicKey publicKey;

    public User(String username, String ip, String port, PublicKey publicKey)  {
        this.username = username;
        this.ip = ip;
        this.port = port;
        this.publicKey = publicKey;
    }

    public User(String username, String ip, String port) throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.username = username;
        this.ip = ip;
        this.port = port;
        getPublicKeyFromFile(username);
    }

    public User(String username){
        this.username = username;
    }

    /**With the username tries to get the user file and save its public key in the instance**/
    private void getPublicKeyFromFile(String username) {
        String fileName = username + ".txt";
        File file = new File(fileName);

        if (!file.exists()) {
            this.publicKey =  null;
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] params = line.split("@");
                String publicKey = params[3];
                this.publicKey = PublicKeyUtils.stringToPublicKey(publicKey);
            }
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public String register(Boolean rewrite) {
        String fileName = username + ".txt"; // Specify the file name
        String content = username + "@" + ip + "@" + port + "@" + PublicKeyUtils.publicKeyToString(publicKey);

        try {
            File file = new File(fileName);

            if (file.exists() && !rewrite) {
                return "User already registered";
            } else {
                file.delete();
            }
            FileWriter fileWriter = new FileWriter(fileName);
            fileWriter.write(content);

            fileWriter.close();
            if(rewrite) {
                System.out.println("File rewritten successfully.");
                return "File rewritten successfully.";

            }
            System.out.println("File created and content written successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "REGISTER@ACK";
    }

    public Boolean verifyMessage(String signatureBase64, String receivedMessage) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] publicKeyBytes = publicKey.getEncoded();

        Signature signature = Signature.getInstance("SHA256withRSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        signature.initVerify(publicKey);

        byte[] receivedSignatureBytes = Base64.getDecoder().decode(signatureBase64);
        signature.update(receivedMessage.getBytes());
        return signature.verify(receivedSignatureBytes);
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public Boolean getUser() {
        String fileName = username + ".txt"; // Specify the file name
        File file = new File(fileName);

        if (!file.exists()) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] params = line.split("@");
                ip = params[1];
                port = params[2];
                publicKey = PublicKeyUtils.stringToPublicKey(params[3]);
            }
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        return true;
    }
}
