package App.Server;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import App.Server.TrackerConnectionHandler;

public class Tracker {
    static java.util.logging.Logger logger = Logger.getLogger(Tracker.class.getName());

    public static void main(String[] args) {
        final int serverPort = 12345;
        final String keystorePassword = "password"; // Only for testing purposes

        try {
            char[] password = keystorePassword.toCharArray();
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream("keystore.jks"), password);

            // Create KeyManagerFactory with the keystore
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, keystorePassword.toCharArray());

            // Create SSLContext and set KeyManagerFactory as the source of authentication keys
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            // Create SSLServerSocketFactory
            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

            // Create SSLServerSocket
            SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(serverPort);

            String[] enabledProtocols = sslServerSocket.getEnabledProtocols();
            sslServerSocket.setEnabledCipherSuites(sslServerSocket.getSupportedCipherSuites());
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
        }
    }
}
