package App.Client;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

// TODO: remove, all functionalities moved to Peer

public class Client {

    private String username = "afonso";
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private BigInteger privateKeyModulus = null;
    private BigInteger privateKeyExponent = null;
    private BigInteger publicKeyModulus = null;
    private BigInteger publicKeyExponent = null;
    private Boolean firstLogin = false;

    private String ip;

    public Client(String ip) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        this.ip = ip;
        getKeysFromFile();
    }

    public void createKeyPair() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey sk = keyPair.getPrivate();
        PublicKey pk = keyPair.getPublic();
        KeyFactory fact = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec priv = fact.getKeySpec(sk,
                RSAPrivateKeySpec.class);
        RSAPublicKeySpec pub = fact.getKeySpec(pk,
                RSAPublicKeySpec.class);
        this.privateKey = sk;
        this.publicKey = pk;
        this.privateKeyModulus = priv.getModulus();
        this.privateKeyExponent = priv.getPrivateExponent();
        this.publicKeyModulus = pub.getModulus();
        this.publicKeyExponent = pub.getPublicExponent();
        storeKeys();
    }

    public void storeKeys() {
        String fileName = "keys.txt";
        String content = privateKeyModulus + "\n" + privateKeyExponent + "\n" + publicKeyModulus + "\n" + publicKeyExponent;

        try {
            File file = new File(fileName);

            if (file.exists()) {
                return;
            }
            FileWriter fileWriter = new FileWriter(fileName);

            fileWriter.write(content);

            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getKeysFromFile() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        String fileName = "keys.txt";

        File file = new File(fileName);

        if (!file.exists()) {
            this.firstLogin = true;
            createKeyPair();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int currentLine = 1;
            while ((line = reader.readLine()) != null) {
                switch (currentLine) {
                    case 1:
                        privateKeyModulus = new BigInteger(line);
                        break;
                    case 2:
                        privateKeyExponent = new BigInteger(line);
                        break;
                    case 3:
                        publicKeyModulus = new BigInteger(line);
                        break;
                    case 4:
                        publicKeyExponent = new BigInteger(line);
                        break;
                }
                currentLine++;
            }
            this.privateKey = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(privateKeyModulus, privateKeyExponent));
            this.publicKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(publicKeyModulus, publicKeyExponent));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String signMessageToTracker(String message) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] privateKeyBytes = privateKey.getEncoded();

        Signature signature = Signature.getInstance("SHA256withRSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        signature.initSign(privateKey);

        signature.update(message.getBytes());
        byte[] signatureBytes = signature.sign();

        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);
        return signatureBase64;
    }

    public void registOrLogin(PrintWriter out) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        if (firstLogin) {
            registInTracker(out);
        } else {
            loginInTracker(out);
        }
    }

    private void registInTracker(PrintWriter out) {
        String message = "regist @ " + username + " @ " + " 12345" + " @ " + publicKeyModulus + " @ " + publicKeyExponent;
        out.println(message);
        System.out.println("Message sent to P2P server: " + message);
    }

    private void loginInTracker(PrintWriter out) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        String signedMessage = signMessageToTracker(ip);
        String message = "login @ " + username + " @ 12345" + " @ " + ip + " @ " + signedMessage;
        out.println(message);
        System.out.println("Message sent to P2P server: " + message);
    }

    public void getUserAddressFromTracker(PrintWriter out,String username) {
        String message = "getUserAddress" + " @ " + username;
        out.println(message);
        System.out.println("Message sent to P2P server: " + message);
    }

    public void deleteFile() {
        File file = new File("keys.txt");
        file.delete();
    }
}
