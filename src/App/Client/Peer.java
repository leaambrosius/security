package App.Client;

import App.Messages.Message;
import App.Messages.MessageHandler;
import App.Messages.MessageType;
import App.Storage.KeyRepository;
import Utils.InvalidMessageException;
import Utils.MessageListener;
import Utils.PublicKeyUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO clean up debug println
 * TODO multiple peers
 * TODO closing sockets
 * TODO refactor, passing host to PeerConnection is meh, we should have separate classes
 */

public class Peer {
    static Logger logger = Logger.getLogger(Peer.class.getName());
    private static final MessageHandler messageHandler = new MessageHandler();

    public String username;
    private KeyPair keyPair;
    private String serverIP;
    private String serverPort;
    private String localPort;
    private ServerSocket connectableSocket;

    private MessageListener listener;

    public HashMap<String, PeerConnection> peerConnections = new HashMap<>();

    public Peer(String username, String serverIP, String serverPort, String localPort) {
        this.username = username;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.localPort = localPort;
    }

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    private String registerToTracker() {
        String encodedMessage = messageHandler.encodeMessage(new Message(MessageType.REGISTER,
                new String[] { username, localPort, PublicKeyUtils.publicKeyToString(keyPair.getPublic()) }));
        logger.log(Level.INFO, "Register message sent to P2P server: " + encodedMessage);
        return sendToServer(encodedMessage);
    }

    private String loginToTracker() throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        String signedMessage = signMessageToTracker(username);
        String encodedMessage = messageHandler.encodeMessage(new Message(MessageType.LOGIN,
                new String[] { username, localPort, signedMessage }));
        logger.log(Level.INFO, "Login message sent to P2P server: " + encodedMessage);
        return sendToServer(encodedMessage);
    }

    public boolean announceToServer() {
        try {
                boolean isFirstLogin = !KeyRepository.keysExist(this.username);
                this.keyPair = KeyRepository.getKeys(this.username);
                String response;

                if (isFirstLogin) {
                    response = registerToTracker();
                } else {
                    response = loginToTracker();
                }

            logger.log(Level.INFO, "Received response from server: " + response);
            Message message = messageHandler.decodeMessage(response);

            if (!message.verifyLength(2) || !message.isAck()) {
                logger.log(Level.WARNING, "Do not received ack message: " + response);
                return false;
            }

            // Open connectable socket for incoming peers connections
            openConnectableSocket();
            listenForConnections();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private String sendToServer(String data) {
        try {

            TrustManager[] trustManagers = {new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            }};

            // Create SSLContext with the custom trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());

            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Create SSL socket and connect to the server
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(serverIP, Integer.parseInt(serverPort));
            String[] enabledProtocols = socket.getEnabledProtocols();
            socket.setEnabledProtocols(enabledProtocols);
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

            socket.startHandshake();
            System.out.println("Connected to server: " + serverIP);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(data);
            String response = in.readLine();
            socket.close();
            return response;

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openConnectableSocket() throws IOException {
        connectableSocket = new ServerSocket(Integer.parseInt(this.localPort));
        logger.log(Level.INFO, "Opened connectable server on port " + this.localPort);
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
        String peerIP = getPeerSocketIP(peerSocket);
        int peerPort = peerSocket.getPort();

        logger.log(Level.INFO, "Incoming peer: " + peerIP + ":" + peerPort);

        PeerConnection newPeerConnection = new PeerConnection(this, keyPair, peerSocket);
        newPeerConnection.setMessageListener(listener);
        newPeerConnection.acceptChat();
    }

    public void connectToPeer(String peerUsername) {
        PeerData peerData = this.getPeerData(peerUsername);
        PeerConnection newPeerConnection = new PeerConnection(this, keyPair, peerData,listener);
        newPeerConnection.setMessageListener(listener);
        boolean isConnectionAccepted = newPeerConnection.announceToPeer(this.username);

        if(isConnectionAccepted) {
            logger.log(Level.INFO, "Connection accepted by peer");

            peerConnections.put(peerUsername, newPeerConnection);
            newPeerConnection.initiateChat();
        } else {
            newPeerConnection.closeConnection();
        }
    }

    // PEER/PEER_IP/PEER_PORT/PEER_PUBLIC_KEY
    public PeerData getPeerData(String peerUsername) {
        try {
            String request = messageHandler.encodeMessage(new Message(MessageType.PEER, new String[] { peerUsername }));
            String response = sendToServer(request);
            Message responseMessage = messageHandler.decodeMessage(response);

            if (!responseMessage.verifyLength(4) || !(responseMessage.getType() == MessageType.PEER)) {
                logger.log(Level.WARNING, "Received invalid response: " + response);
                throw new InvalidMessageException(response);
            }

            String[] responseParts = responseMessage.getParts();
            String peerIP = responseParts[1];
            int peerPort = Integer.parseInt(responseParts[2]);
            PublicKey peerPublicKey = PublicKeyUtils.stringToPublicKey(responseParts[3]);
            logger.log(Level.INFO, "Received peer data from server: " + peerUsername + ":" + peerIP + ":" + peerPort);
            return new PeerData(peerUsername, peerIP, peerPort, peerPublicKey);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String signMessageToTracker(String message) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] privateKeyBytes = this.keyPair.getPrivate().getEncoded();

        // TODO extract encryption stuff to separate module and reuse
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
        if (this.peerConnections.containsKey(peerUsername)) {
            PeerConnection connection = this.peerConnections.get(peerUsername);
            connection.sendMessage(message);
        } else {
            logger.log(Level.WARNING, "Sending message failed: no active connection");
            this.connectToPeer(peerUsername);
            PeerConnection connection = this.peerConnections.get(peerUsername);
            connection.sendMessage(message);
            // TODO verify if that works
        }
    }

    private String getPeerSocketIP(Socket peerSocket) {
        return  peerSocket.getInetAddress().toString().replace("/", "");
    }
}
