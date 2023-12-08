package App.Server;

import App.Client.PeerConnection;
import App.Messages.Message;
import App.Messages.MessageHandler;
import App.Messages.MessageType;
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
    private static final MessageHandler messageHandler = new MessageHandler();

    @Override
    public void run() {
        try {
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            logger.log(Level.INFO, "Connected to P2P client with IP: " + clientIP);

            // Handle incoming data from the client
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = reader.readLine();

            logger.log(Level.INFO,"Received message from P2P client: " + message);

            String instruction = messageHandler.decodeMessage(message).getParts()[0];
            String response = null;

            switch (instruction) {
                case "REGISTER" -> response = registerUser(message, clientIP);
                case "LOGIN" -> response = login(message, clientIP);
                case "PEER" -> response = getUserAddress(message);
                default -> logger.log(Level.WARNING,"Unknown message received: " + message);
            }

            // Send a response to the client
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            writer.println(response);

            writer.close();
            reader.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // REGISTER/USERNAME/PORT/PUBLIC_KEY -> REGISTER/ACK | REGISTER/NACK
    private static String registerUser(String message, String ip) {
        try {
            Message registerMessage = messageHandler.decodeMessage(message);
            String[] params = registerMessage.getParts();
            String username = params[1];
            String port = params[2];
            PublicKey publicKey = PublicKeyUtils.stringToPublicKey(params[3]);

            User user = new User(username, ip, port, publicKey);
            if (user.register(false)) {
                logger.log(Level.INFO,"User registered successfully");
                return messageHandler.generateAck(MessageType.REGISTER);
            }
            return messageHandler.generateNack(MessageType.REGISTER);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return messageHandler.generateNack(MessageType.REGISTER);
        }
    }

    // LOGIN/USERNAME/PORT/SECRET -> LOGIN/ACK | LOGIN/NACK
    private static String login(String message, String ip) {
        try {
            Message loginMessage = messageHandler.decodeMessage(message);
            String[] params = loginMessage.getParts();
            String username = params[1];
            String port = params[2];
            String signedMessage = params[3];

            User user = new User(username, ip, port);

            if (!user.verifyMessage(signedMessage, username)) {
                logger.log(Level.WARNING,"Login contains invalid signature: the message may have been tampered with");
                return messageHandler.generateNack(MessageType.LOGIN);
            }

            user.getUser(false);

            if (user.register(true)) {
                logger.log(Level.INFO,"User logged in successfully");
            }
            return messageHandler.generateAck(MessageType.LOGIN);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
            return messageHandler.generateNack(MessageType.LOGIN);
        }
    }

    // PEER/USERNAME -> PEER/IP/PORT/PUBLIC_KEY | PEER/NACK
    private static String getUserAddress(String message) {
        Message peerMessage = messageHandler.decodeMessage(message);
        String username = peerMessage.getParts()[1];

        User user = new User(username);
        if (!user.getUser(true)) {
            logger.log(Level.WARNING,"Peer message: user not found");
            return messageHandler.generateNack(MessageType.PEER);
        }
        Message response = new Message(MessageType.PEER, new String[] { user.getIp(), user.getPort(), PublicKeyUtils.publicKeyToString(user.getPublicKey())});
        return messageHandler.encodeMessage(response);
    }
}