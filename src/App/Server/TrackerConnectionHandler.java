package App.Server;

import Utils.PublicKeyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

class TrackerConnectionHandler implements Runnable {
    private final Socket clientSocket;

    public TrackerConnectionHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            System.out.println("Connected to P2P client with IP: " + clientIP);

            // Handle incoming data from the client
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = reader.readLine();
            System.out.println("Received message from P2P client: " + message);
            String instruction = message.split("@")[0];
            String response = "NACK";

            switch (instruction) {
                case "REGISTER" -> response = registerUser(message, clientIP);
                case "LOGIN" -> response = login(message, clientIP);
                case "PEER" -> response = getUserAddress(message);
                default -> System.out.println("Unknown message received.");
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

    private static String registerUser(String message,String ip)  {
        try {
            String[] params = message.split("@");
            String username = params[1];
            String port = params[2];
            PublicKey publicKey = PublicKeyUtils.stringToPublicKey(params[3]);
            User user = new User(username, ip, port, publicKey);
            return user.register(false);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return "NACK";
        }
    }

    private static String login(String message, String ip) {
        try {
            String[] params = message.split("@");
            String username = params[1];
            String port = params[2];
            String signedMessage = params[3];
            User user = new User(username, ip, port);
            if(!user.verifyMessage(signedMessage, username)){
                return "Invalid signature; the message may have been tampered with.";
            }
            user.getUser();
            user.register(true);
            return "LOGIN@ACK";
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
            return "NACK";
        }
    }

    private static String getUserAddress(String message) {
        String username = message.split("@")[1];
        User user = new User(username);
        if(!user.getUser()) {
            return "PEER@NACK@USER_NOT_FOUND";
        }
        return "PEER" + "@" + user.getIp() + "@" + user.getPort() + "@" + PublicKeyUtils.publicKeyToString(user.getPublicKey());
    }
}