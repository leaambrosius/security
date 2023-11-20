package App.Client;

import App.Messages.Message;
import App.Messages.MessageHandler;
import App.Messages.MessageType;
import Utils.InvalidMessageException;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PeerConnection {
    static java.util.logging.Logger logger = Logger.getLogger(PeerConnection.class.getName());
    private static final MessageHandler messageHandler = new MessageHandler();

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
            logger.log(Level.WARNING, "Cannot establish connection to peer");
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
            String announcement = messageHandler.encodeMessage(new Message(MessageType.PEER_ANNOUNCEMENT,
                                                                new String[] { username }));
            String ciphertext = encryptMessageWithPeerPublicKey(announcement);
            sendToPeer(ciphertext);
            String encryptedResponse = receiveFromPeer();
            String response = decryptMessageWithPrivateKey(encryptedResponse);
            Message responseMessage = messageHandler.decodeMessage(response);
            if (!(responseMessage.getType() == MessageType.PEER_ANNOUNCEMENT) || !responseMessage.isAck()) {
                throw new InvalidMessageException(response);
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
            Message responseMessage = messageHandler.decodeMessage(plaintext);

            if (!(responseMessage.getType() == MessageType.PEER_ANNOUNCEMENT) || !responseMessage.verifyLength(2)) {
                throw new InvalidMessageException(plaintext);
            }

            String peerUsername = responseMessage.getParts()[1];
            PeerData peerData = this.host.getPeerData(peerUsername);

            if (this.verifyPeer(peerData)) {
                this.peerData = peerData;
                this.host.peerConnections.put(peerUsername, this);
                String ciphertext = encryptMessageWithPeerPublicKey(messageHandler.generateAck(MessageType.PEER_ANNOUNCEMENT));
                sendToPeer(ciphertext);
                logger.log(Level.INFO, "User announcement ack");
            } else {
                logger.log(Level.WARNING, "Peer data does not match with server record, connection not accepted");
            }
        } catch (IOException | InvalidMessageException e) {
            e.printStackTrace();
            this.closeConnection();
        }
    }

    private void initiateHandshake() {
        try {
            // Generate a symmetric key
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            this.symmetricKey = keyGenerator.generateKey();

            String symmetricKeyString = Base64.getEncoder().encodeToString(this.symmetricKey.getEncoded());
            logger.log(Level.INFO, "Setup symmetric key " + Arrays.toString(this.symmetricKey.getEncoded()));

            // Send the encrypted symmetric key to the peer
            Message handshakeAck = new Message(MessageType.HANDSHAKE, new String[] { "ACK", symmetricKeyString });
            String ciphertext = encryptMessageWithPeerPublicKey(messageHandler.encodeMessage(handshakeAck));
            sendToPeer(ciphertext);

            // Wait for the acknowledgment from the peer
            String acknowledgment = decryptMessageWithPrivateKey(receiveFromPeer());
            Message handshakeResponse = messageHandler.decodeMessage(acknowledgment);

            if (!handshakeResponse.isAck()) {
                throw new InvalidMessageException(acknowledgment);
            }
            logger.log(Level.INFO, "Handshake initiation completed. Received ack");

        } catch (NoSuchAlgorithmException | IOException | InvalidMessageException e) {
            logger.log(Level.WARNING, "Handshake initiation timeout. No acknowledgment received");
            this.closeConnection();
            e.printStackTrace();
        }
    }

    public void acceptHandshake() {
        try {
            // Wait for the peer's encrypted symmetric key
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String encryptedMessage = in.readLine();
            System.out.println(encryptedMessage);
            String decryptedMessage = decryptMessageWithPrivateKey(encryptedMessage);
            Message message = messageHandler.decodeMessage(decryptedMessage);

            if (!message.verifyLength(3) || !message.isAck()) {
                throw new InvalidMessageException(decryptedMessage);
            }

            // Reconstruct the symmetric key
            String decryptedSymmetricKeyString = message.getParts()[2];
            byte[] symmetricKeyBytes = Base64.getDecoder().decode(decryptedSymmetricKeyString);
            this.symmetricKey = new SecretKeySpec(symmetricKeyBytes, 0, symmetricKeyBytes.length, "AES");
            logger.log(Level.INFO, "Symmetric key " + Arrays.toString(this.symmetricKey.getEncoded()));

            // Send acknowledgment to the peer
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String handshakeAck = messageHandler.generateAck(MessageType.HANDSHAKE);
            out.println(encryptMessageWithPeerPublicKey(handshakeAck));

        } catch (IOException | InvalidMessageException e) {
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

    private byte[] encryptMessageWithPeerPublicKey(byte[] plaintextBytes) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, peerData.publicKey);
            return cipher.doFinal(plaintextBytes);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return new byte[] {};
    }

    private String encryptMessageWithPeerPublicKey(String plaintext) {
        byte[] plaintextBytes = plaintext.getBytes();
        byte[] encryptedBytes = encryptMessageWithPeerPublicKey(plaintextBytes);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String decryptMessageWithPrivateKey(String ciphertext) {
        byte[] encryptedBytes = Base64.getDecoder().decode(ciphertext);
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, hostKeyPair.getPrivate());
            return new String(cipher.doFinal(encryptedBytes));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String encryptMessageWithSymmetricKey(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);

            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String decryptMessageWithSymmetricKey(String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, symmetricKey);

            byte[] encryptedBytes = Base64.getDecoder().decode(ciphertext);
            return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void startConversation() {
        new Thread(this::listenForMessages).start();
        sendMessages();
    }


    private void sendToPeer(String data) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        logger.log(Level.INFO, "Sending to " + (this.peerData != null ? this.peerData.username : "") + "(" + this.socket.getInetAddress() + ":" + this.socket.getPort() +")" + " from (" + this.socket.getLocalAddress() + ":" + this.socket.getLocalPort() + "): " + data);
        out.println(data);
    }

    private String receiveFromPeer() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String msg = in.readLine();
        logger.log(Level.INFO, "Received from " + (this.peerData != null ? this.peerData.username : "") + "(" + this.socket.getInetAddress() + ":" + this.socket.getPort() +")" + " to (" + this.socket.getLocalAddress() + ":" + this.socket.getLocalPort() + "): " + msg);
        return msg;
    }

    private void listenForMessages() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            socket.setSoTimeout(TIMEOUT_SECONDS * 1000);
            logger.log(Level.INFO, "Started listening for chat messages");
            while (true) {
                try {
                    String encryptedMessage = in.readLine();

                    if (encryptedMessage != null) {
                        String decryptedMessage = this.decryptMessageWithSymmetricKey(encryptedMessage);
                        logger.log(Level.INFO, this.peerData.username + "(" + this.socket.getInetAddress() + ":" + this.socket.getPort() +"): " + decryptedMessage + " received on (" + this.socket.getLocalAddress() + ":" + this.socket.getLocalPort() + ")");
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
                logger.log(Level.INFO, "$(" + this.socket.getLocalAddress() + ":" + this.socket.getLocalPort() + ") to " + (this.peerData != null ? this.peerData.username : "unknown") + "(" + socket.getInetAddress() + ":" + socket.getPort() + "): " + input);
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

    private boolean verifyPeer(PeerData peerData) {
        return Objects.equals(socket.getInetAddress().toString().replace("/", ""), peerData.address);
    }
}
