package App.Server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Server {
    static Logger logger = Logger.getLogger(Tracker.class.getName());

    public static void main(String[] args) {
        final int serverPort = 12345;
        final String keystorePassword = "password"; // Only for testing purposes

        try {
            char[] password = keystorePassword.toCharArray();
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream("keystore.jks"), password);

            SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(serverPort);
            String[] enabledProtocols = sslServerSocket.getEnabledProtocols();
            sslServerSocket.setEnabledProtocols(enabledProtocols);

            logger.log(Level.INFO, "P2P Server is listening on port " + serverPort);

            // Create a thread pool to handle incoming connections
            ExecutorService executor = Executors.newFixedThreadPool(10);

            while (true) {
                SSLSocket clientSocket = (SSLSocket) sslServerSocket.accept();
                // Create a new thread to handle the connection
                Runnable connectionHandler = new TrackerConnectionHandler(clientSocket);
                executor.execute(connectionHandler);
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "P2P Server crushed");

            e.printStackTrace();
        }
    }
}
