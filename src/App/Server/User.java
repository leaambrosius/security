package App.Server;

import Utils.PublicKeyUtils;

import java.io.*;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.*;
import java.util.Base64;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**Server Side user class**/
public class User {
    static java.util.logging.Logger logger = Logger.getLogger(User.class.getName());
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

    public User(String username, String ip, String port) {
        this.username = username;
        this.ip = ip;
        this.port = port;
        readPublicKeyFromFile(username);
    }

    public User(String username){
        this.username = username;
    }

    // Look for user file and try to get their saved public key
    private void readPublicKeyFromFile(String username) {
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

    public boolean register(Boolean rewrite) {
        String fileName = username + ".txt";
        String content = username + "@" + ip + "@" + port + "@" + PublicKeyUtils.publicKeyToString(publicKey);

        try {
            File file = new File(fileName);

            if (file.exists() && !rewrite) {
                logger.log(Level.WARNING, "Cannot register: user already registered");
                return false;
            } else {
                file.delete();
            }
            FileWriter fileWriter = new FileWriter(fileName);
            fileWriter.write(content);
            fileWriter.close();

            if (rewrite) {
                logger.log(Level.INFO, "User registered: file rewritten successfully");
                return true;
            }
            logger.log(Level.INFO, "User registered: file created and content written successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public Boolean verifyMessage(String signatureBase64, String receivedMessage) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] publicKeyBytes = publicKey.getEncoded();

        Signature signature = Signature.getInstance("SHA256withRSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        signature.initVerify(publicKey);

        byte[] receivedSignatureBytes = Base64.getDecoder().decode(signatureBase64); //signatureBase64.getBytes();
        signature.update(receivedMessage.getBytes());
        return signature.verify(receivedSignatureBytes);
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

    public Boolean getUser(boolean useADdressFromFile) {
        String fileName = username + ".txt";
        File file = new File(fileName);

        if (!file.exists()) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] params = line.split("@");
                if(useADdressFromFile) {
                    ip = params[1];
                    port = params[2];
                }
                publicKey = PublicKeyUtils.stringToPublicKey(params[3]);
            }
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        return true;
    }
}
