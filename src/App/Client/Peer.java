package App.Client;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;

import App.Storage.KeyRepository;
import Utils.EnvironmentConfigProvider;
import Utils.InvalidMessageException;
import Utils.PublicKeyUtils;

/**
 * TODO clean up debug println
 * TODO multiple peers
 * TODO closing sockets
 * TODO refactor, passing host to PeerConnection is meh, we should have separate classes
 */

public class Peer {
    private static final int TIMEOUT_SECONDS = 30;

    public String username;
    private KeyPair keyPair;
    private final String serverIP = EnvironmentConfigProvider.getInstance().get("SERVER_ADDRESS");
    private final String serverPort = EnvironmentConfigProvider.getInstance().get("SERVER_PORT");
    private final String localPort = EnvironmentConfigProvider.getInstance().get("PEER_DEFAULT_PORT");
    private ServerSocket connectableSocket;

    public HashMap<String, PeerConnection> peerConnections = new HashMap<>();

    public Peer(String username) {
        this.username = username;
    }

    private String registerToTracker() {
        String message = "REGISTER@" + this.username + "@" + this.localPort + "@" + PublicKeyUtils.publicKeyToString(this.keyPair.getPublic());
        System.out.println("Message sent to P2P server: " + message);

        return sendToServer(message);
    }

    private String loginToTracker() throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        String signedMessage = signMessageToTracker(username);
        String message = "LOGIN@" + username + "@" + localPort + "@" + signedMessage;
        System.out.println("Message sent to P2P server: " + message);

        return sendToServer(message);
    }

    public void announceToServer() {
        try {
            boolean isFirstLogin = !KeyRepository.keysExist();
            this.keyPair = KeyRepository.getKeys();
            String response;

            if (isFirstLogin) {
                response = registerToTracker();
            } else {
                response = loginToTracker();
            }

            System.out.println("Received message from server: " + response);
            String[] responseParts = response.split("@");
            if (responseParts.length == 2 && !Objects.equals(responseParts[1], "ACK")) {
                throw new InvalidMessageException(response);
            }

            // Open connectable socket for incoming peers connections
            openConnectableSocket();
            listenForConnections();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String sendToServer(String data) {
        try {
            Socket socket = new Socket(serverIP, Integer.parseInt(serverPort));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(data);
            String response = in.readLine();
            socket.close();
            return response;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openConnectableSocket() throws IOException {
        connectableSocket = new ServerSocket(Integer.parseInt(this.localPort));
        System.out.println("Opened connectable server on port " + this.localPort);
    }

    private void listenForConnections() {
        new Thread(() -> {
            try {
                while (true) {
                    Socket peerSocket = connectableSocket.accept();
                    new Thread(() -> handleIncomingConnection(peerSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleIncomingConnection(Socket peerSocket) {
        new Thread(() -> {
            String peerIP = peerSocket.getInetAddress().toString().replace("/", "");;
            int peerPort = peerSocket.getPort();
            System.out.println("Incoming peer ip: " + peerIP + " port: " + peerPort);

            PeerConnection newPeerConnection = new PeerConnection(this, keyPair, peerSocket);

            newPeerConnection.acceptChat();
        }).start();
    }

    public void connectToPeer(String peerUsername) {
        PeerData peerData = this.getPeerData(peerUsername);
        PeerConnection newPeerConnection = new PeerConnection(this, keyPair, peerData);
        boolean isConnectionAccepted = newPeerConnection.announceToPeer(this.username);

        if(isConnectionAccepted) {
            System.out.println("Connection accepted by peer");
            peerConnections.put(peerUsername, newPeerConnection);
            newPeerConnection.initiateChat();
        } else {
            newPeerConnection.closeConnection();
        }
    }

    public PeerData getPeerData(String peerUsername) {
        try {
            String request = "PEER@" + peerUsername;
            String response = sendToServer(request);

            // PEER@PEER_IP@PEER_PORT@PEER_PUBLIC_KEY
            String[] responseParts = response.split("@");
            if (responseParts.length != 4 || !Objects.equals(responseParts[0], "PEER")) {
                throw new InvalidMessageException(response);
            }

            String peerIP = responseParts[1];
            int peerPort = Integer.parseInt(responseParts[2]);
            PublicKey peerPublicKey = PublicKeyUtils.stringToPublicKey(responseParts[3]);
            System.out.println("Received peer data from server: " + peerUsername + "@" + peerIP + "@" + peerPort + "@" + peerPublicKey);
            return new PeerData(peerUsername, peerIP, peerPort, peerPublicKey);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String signMessageToTracker(String message) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] privateKeyBytes = this.keyPair.getPrivate().getEncoded();

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

    public void sendMessage(String peerUsername, String message) {
        System.out.println("Sending messages");
        if (this.peerConnections.containsKey(peerUsername)) {
            PeerConnection connection = this.peerConnections.get(peerUsername);
            connection.sendMessage("MESSAGE@" + message);
        } else {
            //TODO handle that case, e.g. establish connection
            System.out.println("No connection with that user yet");
        }
    }
}
