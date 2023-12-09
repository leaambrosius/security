package App.Client;

import App.Messages.*;
import App.Storage.ChatRecord;
import App.Storage.MessagesRepository;
import Utils.InvalidMessageException;
import Utils.MessageListener;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PeerConnection {
    private static final Logger logger = Logger.getLogger(PeerConnection.class.getName());

    private static final int TIMEOUT_MS = 5;
    private final Peer host;
    private Socket socket;
    private PeerData peerData;
    private SecretKey sessionSymmetricKey;
    private SecretKey storageSymmetricKey;
    private MessageListener listener;
    public String chatId;

    public PeerConnection(Peer host, PeerData peerData, MessageListener listener) {
        this.host = host;
        this.peerData = peerData;

        try {
            this.socket = new Socket(peerData.address, Integer.parseInt(peerData.port));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Cannot establish connection to peer");
            listener.userOffline(peerData.username);
        }
    }

    public PeerConnection(Peer host, Socket socket) {
        this.host = host;
        this.socket = socket;
    }

    public boolean initiateChat(String username) {
        if (this.announceToPeer(username)) {
            this.initiateHandshake();
            this.startConversation();
            return true;
        }
        return false;
    }

    public void acceptChat() {
        this.waitForAnnouncement();
        this.acceptHandshake();
        this.startConversation();
    }

    public boolean announceToPeer(String username) {
        try {
            socket.setSoTimeout(TIMEOUT_MS * 1000);
            String announcementMessage = new PeerAnnouncementMessage(username).encode();
            String encryptedMessage = host.encryptionManager.encryptWithPeerPublicKeyToString(announcementMessage, this.peerData.publicKey);
            sendToPeer(encryptedMessage);

            String encryptedResponse = receiveFromPeer();
            String response = host.encryptionManager.decryptWithPrivateKey(encryptedResponse);
            ResponseMessage responseMessage = ResponseMessage.fromString(response);
            if (!responseMessage.isType(MessageType.PEER_ANNOUNCEMENT) || !responseMessage.isAck()) {
                throw new InvalidMessageException(response);
            }
            return true;
        } catch (IOException | InvalidMessageException | InvalidKeyException | IllegalBlockSizeException |
                NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException e) {
            logger.log(Level.WARNING, "Peer announcement failed: " + e);
            this.closeConnection();
            return false;
        }
    }

    public void waitForAnnouncement() {
        try {
            socket.setSoTimeout(TIMEOUT_MS * 1000);
            String announcementMessage = receiveFromPeer();
            String decryptedAnnouncementMessage =  host.encryptionManager.decryptWithPrivateKey(announcementMessage);
            PeerAnnouncementMessage responseMessage = PeerAnnouncementMessage.fromString(decryptedAnnouncementMessage);

            String peerUsername = responseMessage.getUsername();
            PeerData receivedPeerData = host.getPeerData(peerUsername);

            if (this.verifyPeer(receivedPeerData)) {
                peerData = receivedPeerData;
                host.peerConnections.put(peerUsername, this);

                String ack = responseMessage.generateACK();
                String encryptedAck = host.encryptionManager.encryptWithPeerPublicKeyToString(ack, peerData.publicKey);
                sendToPeer(encryptedAck);
                logger.log(Level.INFO, "User announcement ack: " + peerUsername);
            } else {
                logger.log(Level.WARNING, "Peer data does not match with server record, connection not accepted");
            }
        } catch (IOException | InvalidMessageException | InvalidKeyException | IllegalBlockSizeException |
                NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException e) {
            e.printStackTrace();
            this.closeConnection();
        }
    }

    private void initiateHandshake() {
        try {
            // Generate a symmetric key
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            this.sessionSymmetricKey = keyGenerator.generateKey();
            String sessionSymmetricKeyString = Base64.getEncoder().encodeToString(sessionSymmetricKey.getEncoded());
            logger.log(Level.INFO, "Setup session symmetric key: " + Arrays.toString(sessionSymmetricKey.getEncoded()));

            String previousStorageKey = MessagesRepository.mr().getStorageKey(peerData.username);
            if (!previousStorageKey.isEmpty()) {
                byte[] storageKeyBytes = Base64.getDecoder().decode(previousStorageKey);
                this.storageSymmetricKey = new SecretKeySpec(storageKeyBytes, 0, storageKeyBytes.length, "AES");
            } else {
                this.storageSymmetricKey = keyGenerator.generateKey();
            }

            String storageSymmetricKeyString = Base64.getEncoder().encodeToString(storageSymmetricKey.getEncoded());
            logger.log(Level.INFO, "Setup storage symmetric key: " + Arrays.toString(storageSymmetricKey.getEncoded()));

            this.chatId = String.valueOf(storageSymmetricKey.hashCode());
            logger.log(Level.INFO, "Setup chat id: " + this.chatId);

            // Send the encrypted symmetric key to the peer
            String handshakeMessage = new HandshakeMessage(sessionSymmetricKeyString, storageSymmetricKeyString).encode();
            String encryptedHandshakeMessage = host.encryptionManager.encryptWithPeerPublicKeyToString(handshakeMessage, peerData.publicKey);
            sendToPeer(encryptedHandshakeMessage);

            // Wait for the acknowledgment from the peer
            String response = host.encryptionManager.decryptWithPrivateKey(receiveFromPeer());
            ResponseMessage responseMessage = ResponseMessage.fromString(response);

            if (!responseMessage.isAck()) {
                throw new InvalidMessageException(response);
            }
            logger.log(Level.INFO, "Handshake initiation completed. Received ack");

        } catch (IOException | InvalidMessageException | InvalidKeyException | IllegalBlockSizeException |
                NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException e) {
            logger.log(Level.WARNING, "Handshake initiation failed");
            this.closeConnection();
        }
    }

    public void acceptHandshake() {
        try {
            // Wait for the peer's encrypted symmetric key
            String encryptedMessage = receiveFromPeer();
            String decryptedMessage = host.encryptionManager.decryptWithPrivateKey(encryptedMessage);
            HandshakeMessage handshakeMessage = HandshakeMessage.fromString(decryptedMessage);

            // Reconstruct the symmetric keys
            String decryptedSessionSymmetricKeyString = handshakeMessage.getSessionSymmetricKey();
            byte[] symmetricKeyBytes = Base64.getDecoder().decode(decryptedSessionSymmetricKeyString);
            this.sessionSymmetricKey = new SecretKeySpec(symmetricKeyBytes, 0, symmetricKeyBytes.length, "AES");
            logger.log(Level.INFO, "Symmetric key " + Arrays.toString(sessionSymmetricKey.getEncoded()));

            String decryptedStorageSymmetricKeyString = handshakeMessage.getStorageSymmetricKey();
            symmetricKeyBytes = Base64.getDecoder().decode(decryptedStorageSymmetricKeyString);
            this.storageSymmetricKey = new SecretKeySpec(symmetricKeyBytes, 0, symmetricKeyBytes.length, "AES");
            this.chatId = String.valueOf(storageSymmetricKey.hashCode());
            logger.log(Level.INFO, "Storage key " + Arrays.toString(storageSymmetricKey.getEncoded()));
            logger.log(Level.INFO, "Chat ID " + this.chatId);

            this.chatId = String.valueOf(storageSymmetricKey.hashCode());

            // Send acknowledgment to the peer
            String handshakeAck = handshakeMessage.generateACK();
            String encryptedAck = host.encryptionManager.encryptWithPeerPublicKeyToString(handshakeAck, peerData.publicKey);
            sendToPeer(encryptedAck);
        } catch (IOException | InvalidMessageException | InvalidKeyException | IllegalBlockSizeException |
                NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException e) {
            this.closeConnection();
            logger.log(Level.WARNING, "Handshake initiation failed");
        }
    }

    public void sendMessage(String plaintext) {
        try {
            String ciphertext = host.encryptionManager.encryptWithSymmetricKey(plaintext, sessionSymmetricKey);
            sendToPeer(ciphertext);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Sending message to peer failed");
        }
    }

    public void startConversation() {
        logger.log(Level.INFO, "Started listening for chat messages");
        ChatRecord chat = new ChatRecord(peerData.username, chatId, Base64.getEncoder().encodeToString(storageSymmetricKey.getEncoded()));
        MessagesRepository.mr().addChat(chat, this.host);
        new Thread(this::listenForMessages).start();
    }

    private void listenForMessages() {
        try {
            socket.setSoTimeout(TIMEOUT_MS * 1000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                try {
                    String encryptedMessage = in.readLine();
                    if (encryptedMessage != null) {
                        String decryptedMessage = host.encryptionManager.decryptWithSymmetricKey(encryptedMessage, sessionSymmetricKey);
                        Message messageReceived = parseMessage(decryptedMessage);
                        if(listener != null) {
                            listener.messageReceived(messageReceived, peerData.username);
                        }
                        logger.log(Level.INFO, peerData.username + "(" + socket.getInetAddress() + ":" + socket.getPort() +"): " + decryptedMessage + " received on (" + this.socket.getLocalAddress() + ":" + this.socket.getLocalPort() + ")");
                    }
                } catch (SocketTimeoutException e) {
                    // Ignore socket timeout, and continue listening
                }
            }
        } catch (IOException e) {
            //This will make the user close the connection when someone disconnects from the app
            listener.connectionEnded(this);
            logger.log(Level.INFO, "Peer disconnected");
        } catch (InvalidKeyException | IllegalBlockSizeException | NoSuchPaddingException
                | NoSuchAlgorithmException | BadPaddingException | InvalidMessageException e) {
            listener.connectionEnded(this);
            logger.log(Level.WARNING, "Failed to receive the message " + e);

        }
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

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public Message parseMessage(String encodedMessage) throws InvalidMessageException {
        String[] parts = encodedMessage.split("@");
        String messageType = parts[0];

        return switch (messageType) {
            case "MESSAGE" -> ChatMessage.fromString(encodedMessage);
            case "GROUP_MESSAGE" -> GroupMessage.fromString(encodedMessage);
            case "GROUP_INVITATION" -> GroupInvitationMessage.fromString(encodedMessage);
            default -> {
                logger.log(Level.WARNING, "Unknown message type: " + messageType);
                yield null;
            }
        };
    }
}
