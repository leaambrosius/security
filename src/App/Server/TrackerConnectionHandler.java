package App.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

class TrackerConnectionHandler implements Runnable {
    private Socket clientSocket;

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
            String instruction = message.split(" @ ")[0];
            String response = "";
            if(instruction.equals("regist")) {
               response =  registUser(message,clientIP);
            } else if (instruction.equals("login")) {
                response = login(message,clientIP);
            } else if (instruction.equals("getUserAddress")) {
                response = getUserAddress(message);
            }

            // Send an response to the client
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            writer.println(response);

            writer.close();
            reader.close();
            clientSocket.close();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | SignatureException |
                 InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private static String registUser(String message,String ip) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String[] params = message.split(" @ ");
        String username = params[1];
        String port = params[2];
        BigInteger pkModulus = new BigInteger(params[3]);
        BigInteger pkExponent = new BigInteger(params[4]);
        User user = new User(username,ip,port,pkModulus,pkExponent);
        return user.regist(false);
    }

    private static String login(String message,String ip) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        String[] params = message.split(" @ ");
        String username = params[1];
        String port = params[2];
        String messageToConfirm = params[3]; //it's the "ip"
        String signedMessage = params[4];
        User user = new User(username,ip,port);
        if(!user.verifyMessage(signedMessage,messageToConfirm)){
            return "Invalid signature; the message may have been tampered with.";
        }
        user.getUser();
        user.regist(true);
        return "Signature is valid; the message is authentic.";
    }

    private static String getUserAddress(String message) {
        String username = message.split(" @ ")[1];
        User user = new User(username);
        if(!user.getUser()) {
            return "No user found!";
        }
        return user.getIp() + " @ "+ user.getPort() + " @ "+ user.getPublicKeyModulus() + " @ " + user.getPublicKeyExponent();
    }
}