package App.Server;

import App.Messages.*;
import App.Storage.RemoteStorage;
import App.Storage.StorageMessage;
import Utils.InvalidMessageException;
import Utils.PublicKeyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

record TrackerConnectionHandler(Socket clientSocket) implements Runnable {
    static java.util.logging.Logger logger = Logger.getLogger(TrackerConnectionHandler.class.getName());

    @Override
    public void run() {
        try {
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            logger.log(Level.INFO, "Connected to P2P client with IP: " + clientIP);

            // Handle incoming data from the client
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = reader.readLine();

            logger.log(Level.INFO,"Received message from P2P client: " + message);

            String type = checkMessageType(message);
            String response = null;

            switch (type) {
                case "REGISTER" -> response = registerUser(message, clientIP);
                case "LOGIN" -> response = login(message, clientIP);
                case "PEER_DISCOVER" -> response = getUserAddress(message);

                case "GET_CHAT" -> response = getChatHistory(message);
                case "REGISTER_CHAT" -> response = registerChatInStorage(message);
                case "STORE_CHAT" -> response = storeMessagesInStorage(message);
                case "SEARCH_CHAT" -> response = searchMessage(message);
                case "STORE_KEYWORD" -> response = storeKeyword(message);
                default -> logger.log(Level.WARNING,"Unknown message received: " + message);
            }

            // Send a response to the client
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            writer.println(response);

            writer.close();
            reader.close();
            clientSocket.close();
        } catch (IOException | InvalidMessageException e) {
            logger.log(Level.WARNING, "Failed to read: " + e);
        }
    }

    // REGISTER/USERNAME/PORT/PUBLIC_KEY -> REGISTER/ACK | REGISTER/NACK
    private static String registerUser(String message, String ip) {
        try {
            RegisterMessage registerMessage = RegisterMessage.fromString(message);

            String username = registerMessage.getUsername();
            String port = registerMessage.getLocalPort();
            PublicKey publicKey = PublicKeyUtils.stringToPublicKey(registerMessage.getPublicKey());

            User user = new User(username, ip, port, publicKey);
            if (user.register(false)) {
                logger.log(Level.INFO,"User registered successfully");
                return registerMessage.generateACK();
            }
            return registerMessage.generateNACK();
        } catch (GeneralSecurityException | InvalidMessageException e) {
            logger.log(Level.WARNING, "Invalid register message received");
            return RegisterMessage.getNACK();
        }
    }

    // LOGIN/USERNAME/PORT/SECRET -> LOGIN/ACK | LOGIN/NACK
    private static String login(String message, String ip) {
        try {
            LoginMessage loginMessage = LoginMessage.fromString(message);
            String username = loginMessage.getUsername();
            String port = loginMessage.getLocalPort();
            String signature = loginMessage.getSignature();

            User user = new User(username, ip, port);

            if (!user.verifySignature(signature, username + "@" + port)) {
                logger.log(Level.WARNING,"Login contains invalid signature: the message may have been tampered with");
                return loginMessage.generateNACK();
            }

            user.getUser(false);

            if (user.register(true)) {
                logger.log(Level.INFO,"User logged in successfully");
            }
            return loginMessage.generateACK();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | SignatureException | InvalidKeyException | InvalidMessageException e) {
            e.printStackTrace();
            return LoginMessage.getNACK();
        }
    }

    // PEER/USERNAME -> PEER/IP/PORT/PUBLIC_KEY | PEER/NACK
    private static String getUserAddress(String message) {
        try {
            PeerDiscoverMessage peerMessage = PeerDiscoverMessage.fromString(message);
            String username = peerMessage.getPeerUsername();

            User user = new User(username);
            if (!user.getUser(true)) {
                logger.log(Level.WARNING, "Peer message: user not found");
                return peerMessage.generateNACK();
            }
            PeerDataMessage response = new PeerDataMessage(user.getIp(), user.getPort(), PublicKeyUtils.publicKeyToString(user.getPublicKey()));
            return response.encode();
        } catch (InvalidMessageException e) {
            logger.log(Level.WARNING, "Received invalid peer discover message");
            return PeerDiscoverMessage.getNACK();
        }
    }

    private String checkMessageType(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length > 0) return parts[0];

        throw new InvalidMessageException("Invalid message type");
    }

    private String getChatHistory(String message) {
        try {
            GetChatMessage getChatMessage = GetChatMessage.fromString(message);
            String username = getChatMessage.getUsername();
            String chatId = getChatMessage.getChatId();
            String signature = getChatMessage.getSignature();

            User user = new User(username);
            if (!user.getUser(true)) {
                logger.log(Level.WARNING, "Invalid user");
                return HistoryMessage.getNACK();
            }

            if (user.verifySignature(signature, chatId + "@" + username) && RemoteStorage.hasUserAccessToChat(username, chatId)) {
                ArrayList<String> messages = RemoteStorage.getChatMessages(chatId);
                String serializedMessages = String.join("@", messages);
                HistoryMessage response = new HistoryMessage(chatId, serializedMessages);
                return response.encode();
            }
            return HistoryMessage.getNACK();
        } catch (InvalidMessageException | InvalidKeyException | SignatureException | NoSuchAlgorithmException | InvalidKeySpecException | SQLException e) {
            logger.log(Level.WARNING, "Failed to get messages from store " + e);
            return HistoryMessage.getNACK();
        }
    }

    private String storeMessagesInStorage(String message) {
        try {
            StoreChatMessage storeChatMessage = StoreChatMessage.fromString(message);
            String username = storeChatMessage.getUsername();
            String chatId = storeChatMessage.getChatId();
            String signature = storeChatMessage.getSignature();
            ArrayList<String> messages = storeChatMessage.getSerializedDataList();

            User user = new User(username);
            if (!user.getUser(true)) {
                logger.log(Level.WARNING, "Invalid user");
                return storeChatMessage.generateNACK();
            }
            if (!user.verifySignature(signature, chatId + "@" + username)) {
                logger.log(Level.WARNING, "Invalid signature");
                return storeChatMessage.generateNACK();
            }
            if (RemoteStorage.hasUserAccessToChat(username, chatId)) {
                ArrayList<StorageMessage> storageMessages = new ArrayList<>();

                for (String m : messages) {
                    storageMessages.add(StorageMessage.deserialize(m));
                }
                RemoteStorage.insertMessages(storageMessages);
                return storeChatMessage.generateACK();
            }
            logger.log(Level.WARNING, "No access to chat");
            return storeChatMessage.generateNACK();
        } catch (InvalidMessageException | ClassNotFoundException | IOException | SQLException | InvalidKeyException | SignatureException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.log(Level.WARNING, "Failed to store messages in store " + e);
            return StoreChatMessage.getNACK();
        }
    }

    private String registerChatInStorage(String message) {
        try {
            RegisterChatMessage registerChatMessage = RegisterChatMessage.fromString(message);
            String username = registerChatMessage.getUsername();
            String chatId = registerChatMessage.getChatId();
            String signature = registerChatMessage.getSignature();

            User user = new User(username);
            if (!user.getUser(true)) {
                logger.log(Level.WARNING, "Invalid user");
                return registerChatMessage.generateNACK();
            }
            if (user.verifySignature(signature, chatId + "@" + username)) {
                RemoteStorage.insertChat(chatId, username);
                return registerChatMessage.generateACK();
            }
            return registerChatMessage.generateNACK();
        } catch (InvalidMessageException | InvalidKeyException | SignatureException | NoSuchAlgorithmException | InvalidKeySpecException | SQLException e) {
            logger.log(Level.WARNING, "Failed to register chat to store " + e);
            return RegisterChatMessage.getNACK();
        }
    }

    private String searchMessage(String message) {
        try {
            SearchChatMessage searchChatMessage = SearchChatMessage.fromString(message);
            String username = searchChatMessage.getUsername();
            String chatId = searchChatMessage.getChatId();
            String signature = searchChatMessage.getSignature();
            String encryptedKeyword = searchChatMessage.getEncryptedKeyword();

            User user = new User(username);
            if (!user.getUser(true)) {
                logger.log(Level.WARNING, "Invalid user");
                return SearchingResultMessage.getNACK();
            }

            if (user.verifySignature(signature, chatId + "@" + username+ "@" +encryptedKeyword) && RemoteStorage.hasUserAccessToChat(username, chatId)) {
                ArrayList<String> messageIds = RemoteStorage.getMessagesForKeywordAndChatId(encryptedKeyword, chatId);
                String serializedMessages = String.join("@", messageIds);
                SearchingResultMessage response = new SearchingResultMessage(chatId, serializedMessages);
                return response.encode();
            }
            return SearchingResultMessage.getNACK();
        } catch (InvalidMessageException | InvalidKeyException | SignatureException | NoSuchAlgorithmException | InvalidKeySpecException | SQLException e) {
            logger.log(Level.WARNING, "Failed to get messages from store " + e);
            return SearchingResultMessage.getNACK();
        }
    }

    private String storeKeyword(String message) {
        try {
            StoreKeywordsMessage storeKeywordsMessage = StoreKeywordsMessage.fromString(message);
            String username = storeKeywordsMessage.getUsername();
            String chatId = storeKeywordsMessage.getChatId();
            String signature = storeKeywordsMessage.getSignature();
            ArrayList<String> messages = storeKeywordsMessage.getSerializedDataList();
            String messageId = storeKeywordsMessage.getMessageId();

            User user = new User(username);
            if (!user.getUser(true)) {
                logger.log(Level.WARNING, "Invalid user");
                return storeKeywordsMessage.generateNACK();
            }

            String encodedKeywords = String.join("@", messages);
            if (!user.verifySignature(signature, chatId + "@" + username + "@" + messageId + "@" + encodedKeywords)) {
                logger.log(Level.WARNING, "Invalid signature");
                return storeKeywordsMessage.generateNACK();
            }
            if (RemoteStorage.hasUserAccessToChat(username, chatId)) {
                RemoteStorage.insertKeywordMessages(messages, messageId);
                return storeKeywordsMessage.generateACK();
            }
            logger.log(Level.WARNING, "No access to chat");
            return storeKeywordsMessage.generateNACK();
        } catch (InvalidMessageException | SQLException | InvalidKeyException | SignatureException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.log(Level.WARNING, "Failed to store messages in store " + e);
            return StoreKeywordsMessage.getNACK();
        }
    }
}
