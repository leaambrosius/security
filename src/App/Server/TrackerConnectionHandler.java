package App.Server;

import App.Messages.*;
import Utils.InvalidMessageException;
import Utils.PublicKeyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
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
                default -> logger.log(Level.WARNING,"Unknown message received: " + message);
            }

            // Send a response to the client
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            writer.println(response);

            writer.close();
            reader.close();
            clientSocket.close();
        } catch (IOException | InvalidMessageException e) {
            e.printStackTrace();
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
}