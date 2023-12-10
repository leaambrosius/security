package App.Client;

import App.Messages.*;
import App.SearchableEncryption.SearchingManager;
import App.Storage.KeyRepository;
import App.Storage.MessagesRepository;
import App.Storage.StorageMessage;
import Utils.InvalidMessageException;
import Utils.MessageListener;
import Utils.PublicKeyUtils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Peer {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    static Instant lastReadData;
    static Instant lastStoreData;

    static Logger logger = Logger.getLogger(Peer.class.getName());

    private final String serverIP;
    private final String serverPort;
    private final String localPort;
    public final String username;

    private final KeyPair keyPair;
    public final EncryptionManager encryptionManager;

    private ServerSocket connectableSocket;
    private MessageListener listener;

    public HashMap<String, PeerConnection> peerConnections = new HashMap<>();

    public Peer(String username, String serverIP, String serverPort, String localPort) throws Exception {
        this.username = username;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.localPort = localPort;

        boolean isFirstLogin = !KeyRepository.keysExist(this.username);
        this.keyPair = KeyRepository.getKeys(this.username);
        this.encryptionManager = new EncryptionManager(this.keyPair);
        this.announceToServer(isFirstLogin);
    }

    private void announceToServer(boolean isFirstLogin) throws Exception {
        String response = isFirstLogin ?  registerToTracker() : loginToTracker();
        logger.log(Level.INFO, "Received response from server: " + response);

        ResponseMessage registerResponse = ResponseMessage.fromString(response);

        if (!registerResponse.isAck()) {
            throw new Exception("Server refused connection: " + response);
        }

        // Open connectable socket for incoming peers connections
        openConnectableSocket();
        listenForConnections();
    }

    private String registerToTracker() {
        String registerMessage = new RegisterMessage(username, localPort, PublicKeyUtils.publicKeyToString(keyPair.getPublic())).encode();
        logger.log(Level.INFO, "Register message sent to P2P server: " + registerMessage);
        return sendToServer(registerMessage);
    }

    private String loginToTracker() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        String signature = encryptionManager.signMessage(username + "@" + localPort);
        LoginMessage loginMessage = new LoginMessage(username, localPort, signature);
        String encodedMessage = loginMessage.encode();
        logger.log(Level.INFO, "Login message sent to P2P server: " + encodedMessage);
        return sendToServer(encodedMessage);
    }

    public void connectToPeer(String peerUsername) {
        logger.log(Level.INFO, "Connecting to: " + peerUsername);

        PeerData peerData = this.getPeerData(peerUsername);
        PeerConnection newPeerConnection = new PeerConnection(this, peerData,listener);
        newPeerConnection.setMessageListener(listener);

        if(newPeerConnection.initiateChat(this.username)) {
            peerConnections.put(peerUsername, newPeerConnection);
            logger.log(Level.INFO, "Connection accepted by peer");
        } else {
            newPeerConnection.closeConnection();
            logger.log(Level.WARNING, "Connection refused by peer");
        }
    }

    public PeerData getPeerData(String peerUsername) {
        try {
            PeerDiscoverMessage peerDiscoverMessage = new PeerDiscoverMessage(peerUsername);
            String encodedMessage = peerDiscoverMessage.encode();
            String serverResponse = sendToServer(encodedMessage);
            PeerDataMessage responseMessage = PeerDataMessage.fromString(serverResponse);

            String peerIP = responseMessage.getPeerIp();
            String peerPort = responseMessage.getPeerPort();
            PublicKey peerPublicKey = PublicKeyUtils.stringToPublicKey(responseMessage.getPeerPublicKey());
            logger.log(Level.INFO, "Received peer data from server: " + peerUsername + ":" + peerIP + ":" + peerPort);
            return new PeerData(peerUsername, peerIP, peerPort, peerPublicKey);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Received invalid peer data from server: " + e);
            return null;
        }
    }

    public void registerChatToRemote(String chatId) {
        executorService.submit(() -> {
            try {
                String signature = encryptionManager.signMessage(chatId + "@" + username);
                RegisterChatMessage registerChatMessage = new RegisterChatMessage(username, chatId, signature);
                String serverResponse = sendToServer(registerChatMessage.encode());

                ResponseMessage response = ResponseMessage.fromString(serverResponse);
                if (response.isAck()) logger.log(Level.INFO, "Chat registered remotely successfully");
                else logger.log(Level.WARNING, "Failed to register chat remotely");

            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | InvalidMessageException e) {
                logger.log(Level.WARNING, "Failed to register chat remotely " + e);
            }
        });
    }

    public void loadMessagesFromRemoteServer(String chatId) {
        if (wasAccessedWithin10s(lastReadData)) return;

        lastReadData = Instant.now();
        executorService.submit(() -> {
            try {
                String signature = encryptionManager.signMessage(chatId + "@" + username);
                GetChatMessage getChatMessage = new GetChatMessage(username, chatId, signature);
                String serverResponse = sendToServer(getChatMessage.encode());
                HistoryMessage historyMessage = HistoryMessage.fromString(serverResponse);
                ArrayList<String> serializedMessages = historyMessage.getSerializedDataList();
                ArrayList<StorageMessage> messages = new ArrayList<>();

                for (String m : serializedMessages) {
                    StorageMessage storageMessage = StorageMessage.deserialize(m);
                    SecretKey secretKey = getStorageKeyByChat(chatId);

                    if (secretKey == null || storageMessage == null) {
                        logger.log(Level.WARNING, "Invalid key or remote message");
                        return;
                    }

                    storageMessage.decrypt(encryptionManager, secretKey);
                    messages.add(storageMessage);
                }
                MessagesRepository.mr().addMultipleMessages(chatId, messages);
            } catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | IOException | InvalidKeyException | SignatureException | InvalidMessageException | ClassNotFoundException e) {
                logger.log(Level.WARNING, "Failed to fetch chat history " + e);
            }
        });
    }

    public void sendMessagesToRemoteServer(String chatId) {
        if (wasAccessedWithin10s(lastStoreData)) return;

        lastStoreData = Instant.now();
        executorService.submit(() -> {
            ArrayList<StorageMessage> storageMessages = MessagesRepository.mr().getChatHistoryForRemote(chatId);
            ArrayList<String> serializedMessages = new ArrayList<>();
            SecretKey secretKey = getStorageKeyByChat(chatId);
            if (secretKey == null) {
                logger.log(Level.WARNING, "Invalid storage key");
            }
            try {
                for (StorageMessage message : storageMessages) {
                    StorageMessage encrypted = message.encrypted(encryptionManager, secretKey);
                    String serialized = encrypted.serialize();
                    serializedMessages.add(serialized);
                    sendKeywords(message);
                }
                String signature = encryptionManager.signMessage(chatId + "@" + username);
                StoreChatMessage storeChatMessage = new StoreChatMessage(username, chatId, signature, serializedMessages);
                String serverResponse = sendToServer(storeChatMessage.encode());
                ResponseMessage response = ResponseMessage.fromString(serverResponse);

                if (response.isAck()) {
                    MessagesRepository.mr().updateStored(chatId, storageMessages);
                    logger.log(Level.INFO, "Messages stored remotely successfully");
                }
                else logger.log(Level.WARNING, "Failed to store messages remotely");

            } catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | IOException | InvalidKeyException | SignatureException | InvalidMessageException e) {
                logger.log(Level.WARNING, "Failed to send chat history " + e);
            }
        });
    }

    private void sendKeywords(StorageMessage m) {
        executorService.submit(() -> {
            ArrayList<String> plaintextKeywords = SearchingManager.getKeywords(m.message);
            ArrayList<String> keywords = new ArrayList<>();
            for (String word : plaintextKeywords) {
                keywords.add(encryptionManager.getKeywordHash(word));
            }
            try {
                String encodedKeywords = String.join("@", keywords);
                String signature = encryptionManager.signMessage(m.chatId + "@" + username + "@" + m.messageId + "@" + encodedKeywords);
                StoreKeywordsMessage storeKeywordsMessage = new StoreKeywordsMessage(username, m.chatId, signature, keywords, m.messageId);
                String serverResponse = sendToServer(storeKeywordsMessage.encode());
                ResponseMessage response = ResponseMessage.fromString(serverResponse);
                if (response.isAck()) logger.log(Level.INFO, "Keywords stored remotely successfully");
                else logger.log(Level.WARNING, "Failed to store keywords remotely");

            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | InvalidMessageException e) {
                logger.log(Level.WARNING, "Failed to send keywords history " + e);
            }
        });
    }

    private String sendToServer(String encodedMessage) {
        try {
            SSLSocket socket = encryptionManager.getSSLSocket(serverIP, serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(encodedMessage);
            String response = in.readLine();
            socket.close();
            return response;

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            logger.log(Level.SEVERE, "Message not send to server: " + encodedMessage);
            return null;
        }
    }

    private SecretKey getStorageKeyByChat(String chatId) {
        String key = MessagesRepository.mr().getStorageKeyByChat(chatId);
        if (key == null) return null;
        logger.log(Level.INFO, "storage key " + key);
        byte[] keyBytes = Base64.getDecoder().decode(key);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
    }

    private void openConnectableSocket() throws IOException {
        connectableSocket = new ServerSocket(Integer.parseInt(this.localPort));
        logger.log(Level.INFO, "Opened connectable server on port: " + this.localPort);
    }

    private void listenForConnections() {
        new Thread(() -> {
            try {
                while (true) {
                    Socket peerSocket = connectableSocket.accept();
                    new Thread(() -> handleIncomingConnection(peerSocket)).start();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error while waiting for peer connections: " + e);
            }
        }).start();
    }

    private void handleIncomingConnection(Socket peerSocket) {
        String peerIP = getPeerSocketIP(peerSocket);
        int peerPort = peerSocket.getPort();

        logger.log(Level.INFO, "Incoming peer: " + peerIP + ":" + peerPort);

        PeerConnection newPeerConnection = new PeerConnection(this, peerSocket);
        newPeerConnection.setMessageListener(listener);
        newPeerConnection.acceptChat();
    }

    private String getPeerSocketIP(Socket peerSocket) {
        return  peerSocket.getInetAddress().toString().replace("/", "");
    }

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    private boolean wasAccessedWithin10s(Instant access) {
        Instant now = Instant.now();
        Instant minuteAgo = now.minusSeconds(10);
        return access != null && access.isAfter(minuteAgo);
    }

    public void searchForKeyword( String chatId, String keyword, String fullKeyword){
        executorService.submit(() -> {
            try {
                String encryptedKeyword = encryptionManager.getKeywordHash(keyword);
                String signature = encryptionManager.signMessage(chatId + "@" + username+ "@" + encryptedKeyword);
                SearchChatMessage searchChatMessage = new SearchChatMessage(username, chatId, encryptedKeyword, signature);
                String serverResponse = sendToServer(searchChatMessage.encode());
                SearchingResultMessage searchingResultMessage = SearchingResultMessage.fromString(serverResponse);
                ArrayList<String> serializedMessageIds = searchingResultMessage.getSerializedDataList();
                SearchingManager.putMessages(serializedMessageIds, chatId, fullKeyword);
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | InvalidMessageException e) {
                logger.log(Level.WARNING, "Failed to fetch search results " + e);
            }
        });

    }
}
