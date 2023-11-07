package App.Server;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.*;
import java.util.Base64;
import java.security.*;

/**Server Side user class**/
public class User {
    private String username;
    private String ip;
    private String port;
    private PublicKey publicKey;
    private BigInteger publicKeyModulus;
    private BigInteger publicKeyExponent;

    public User(String username, String ip, String port,BigInteger publicKeyModulus,BigInteger publicKeyExponent) throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.username = username;
        this.ip = ip;
        this.port = port;
        //this.publicKey = convertStringToPublicKey(publicKey);
        this.publicKeyModulus = publicKeyModulus;
        this.publicKeyExponent = publicKeyExponent;
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
    private void getPublicKeyFromFile(String username) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String fileName = username+".txt";

        File file = new File(fileName);

        if (!file.exists()) {
            this.publicKey =  null;
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] params = line.split(" @ ");
                BigInteger pkModulus = new BigInteger(params[3]);
                BigInteger pkExponent = new BigInteger(params[4]);
                this.publicKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(pkModulus,pkExponent));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**Regist new users**/
    public String regist(Boolean rewrite) {
        String fileName = username+".txt"; // Specify the file name
        String content = username + " @ " + ip + " @ " + port+ " @ " + publicKeyModulus + " @ " + publicKeyExponent;

        try {
            File file = new File(fileName);

            if (file.exists() && !rewrite) {
                return "User already registered";
            }else {
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
        return "User registered successfully";
    }

    public Boolean verifyMessage(String signatureBase64,String receivedMessage) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] publicKeyBytes = publicKey.getEncoded();

        Signature signature = Signature.getInstance("SHA256withRSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        signature.initVerify(publicKey);

        byte[] receivedSignatureBytes = Base64.getDecoder().decode(signatureBase64);
        signature.update(receivedMessage.getBytes());
        boolean verified = signature.verify(receivedSignatureBytes);

        if (verified) {
            return true;
        } else {
            return false;
        }
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
        String fileName = username+".txt"; // Specify the file name
        File file = new File(fileName);

        if (!file.exists()) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] params = line.split(" @ ");
                ip = params[1];
                port = params[2];
                publicKeyModulus = new BigInteger(params[3]);
                publicKeyExponent = new BigInteger(params[4]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public BigInteger getPublicKeyModulus() {
        return publicKeyModulus;
    }

    public BigInteger getPublicKeyExponent() {
        return publicKeyExponent;
    }
}
