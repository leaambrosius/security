package App.Client;

import Utils.InvalidMessageException;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.*;
import java.util.Base64;
import java.util.Objects;

public class PeerConnection {
    private static final int TIMEOUT_SECONDS = 5;
    private final Peer host;
    private final KeyPair hostKeyPair;

    private PeerData peerData;
    private Socket socket;
    private SecretKey symmetricKey;

    public PeerConnection(Peer host, KeyPair hostKeyPair, PeerData peerData) {
        this.host = host;
        this.hostKeyPair = hostKeyPair;
        this.peerData = peerData;

        try {
            this.socket = new Socket(peerData.address, peerData.port);
        } catch (IOException e) {
            System.out.println("DEBUG: cannot establish connection to peer.");
            e.printStackTrace();
        }
    }

    public PeerConnection(Peer host, KeyPair hostKeyPair, Socket socket) {
        this.host = host;
        this.hostKeyPair = hostKeyPair;
        this.socket = socket;
    }

    public void initiateChat() {
        this.initiateHandshake();
        this.startConversation();
    }

    public void acceptChat() {
        this.waitForAnnouncement();
        this.acceptHandshake();
        this.startConversation();
    }

    public boolean announceToPeer(String username) {
        try {
            socket.setSoTimeout(TIMEOUT_SECONDS * 1000);
            String announcement = "PEER_ANNOUNCEMENT@" + username;
            String ciphertext = encryptMessageWithPublicKey(announcement);
            sendToPeer(ciphertext);
            String response = receiveFromPeer();
            String plaintext = decryptMessageWithPrivateKey(response);
            if (!Objects.equals(plaintext, "PEER_ANNOUNCEMENT@ACK")) {
                throw new InvalidMessageException(plaintext);
            }
            return true;
        } catch (IOException | InvalidMessageException e) {
            e.printStackTrace();
            this.closeConnection();
            return false;
        }
    }

    public void waitForAnnouncement() {
        try {
            socket.setSoTimeout(TIMEOUT_SECONDS * 1000);
            String announcement = receiveFromPeer();
            String plaintext = decryptMessageWithPrivateKey(announcement);

            String[] plaintextParts = plaintext.split("@");
            if (plaintextParts.length != 2 && !Objects.equals(plaintextParts[0], "PEER_ANNOUNCEMENT")) {
                throw new InvalidMessageException(plaintext);
            }

            String peerUsername = plaintextParts[1];
            PeerData peerData = this.host.getPeerData(peerUsername);

            if (this.verifyPeer(peerData)) {
                this.peerData = peerData;
                this.host.peerConnections.put(peerUsername, this);
                String ciphertext = encryptMessageWithPublicKey("PEER_ANNOUNCEMENT@ACK");
                sendToPeer(ciphertext);
            } else {
                System.out.println("DEBUG: peer data does not match with server record, connection not accepted.");
            }
        } catch (IOException | InvalidMessageException e) {
            e.printStackTrace();
            this.closeConnection();
        }
    }

    private boolean verifyPeer(PeerData peerData) {
        return Objects.equals(socket.getInetAddress().toString().replace("/", ""), peerData.address);
    }

    private void initiateHandshake() {
        try {
            // Generate a symmetric key
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            this.symmetricKey = keyGenerator.generateKey();

            byte[] symmetricKeyBytes = this.symmetricKey.getEncoded();

            // Encrypt the symmetric key with the peer's public key using RSA
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, peerData.publicKey);
            byte[] encryptedSymmetricKeyBytes = cipher.doFinal(symmetricKeyBytes);

            // Send the encrypted symmetric key to the peer
            sendToPeer("HANDSHAKE@ACK@" +  Base64.getEncoder().encodeToString(encryptedSymmetricKeyBytes));

            // Wait for the acknowledgment from the peer
            String acknowledgment = receiveFromPeer();
            System.out.println("Received handshake ack");

            // Process the acknowledgment as needed
            if (!Objects.equals(acknowledgment, "HANDSHAKE@ACK")) {
                throw new InvalidMessageException(acknowledgment);
            }
        } catch (NoSuchAlgorithmException | IllegalBlockSizeException | NoSuchPaddingException | BadPaddingException | IOException | InvalidKeyException | InvalidMessageException e) {
            System.out.println("Handshake initiation timeout. No acknowledgment received.");
            this.closeConnection();
            e.printStackTrace();
        }
    }

    public void acceptHandshake() {
        try {
            // Wait for the peer's encrypted symmetric key
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message = in.readLine();
            String [] messageParts = message.split("@");

            if (messageParts.length != 3 || !Objects.equals(messageParts[1], "ACK")) {
                throw new InvalidMessageException(message);
            }
            String encryptedSymmetricKeyString = messageParts[2];
            byte[] encryptedSymmetricKeyBytes = Base64.getDecoder().decode(encryptedSymmetricKeyString);

            // Decrypt the symmetric key using the private key
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, hostKeyPair.getPrivate());
            byte[] symmetricKeyBytes = cipher.doFinal(encryptedSymmetricKeyBytes);

            // Reconstruct the symmetric key
            this.symmetricKey = new SecretKeySpec(symmetricKeyBytes, 0, symmetricKeyBytes.length, "AES");

            // Send acknowledgment to the peer
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("HANDSHAKE@ACK");

        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidMessageException e) {
            this.closeConnection();
            e.printStackTrace();
        }
    }

    public void sendMessage(String plaintext) {
        try {
            String ciphertext = encryptMessageWithSymmetricKey(plaintext);
            sendToPeer(ciphertext);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String encryptMessageWithPublicKey(String plaintext) {
        // TODO
        return plaintext;
    }

    private String encryptMessageWithSymmetricKey(String plaintext) {
        // TODO
        return plaintext;
    }

    private String decryptMessageWithPrivateKey(String ciphertext) {
        // TODO
        return ciphertext;
    }

    private String decryptMessageWithSymmetricKey(String ciphertext) {
        // TODO
        return ciphertext;
    }

    public void startConversation() {
        new Thread(this::listenForMessages).start();
        sendMessages();
    }

    private void sendToPeer(String data) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(data);
        System.out.println("Send message to peer " + data + " on " + socket.getInetAddress() + ":" + socket.getPort());
    }

    private String receiveFromPeer() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return in.readLine();
    }

    private void listenForMessages() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            socket.setSoTimeout(TIMEOUT_SECONDS * 1000);
            System.out.println("========= Chat started ========= ");
            while (true) {
                try {
                    String encryptedMessage = in.readLine();

                    if (encryptedMessage != null) {
                        String decryptedMessage = this.decryptMessageWithSymmetricKey(encryptedMessage);

                        System.out.println(this.peerData.username + ": " + decryptedMessage);
                    }
                } catch (SocketTimeoutException e) {
                    // Ignore socket timeout, and continue listening
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessages() {
        try {
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String input;

            while ((input = userInput.readLine()) != null) {
                String encryptedMessage = encryptMessageWithSymmetricKey(input);
                out.println(encryptedMessage);
            }
            closeConnection();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
