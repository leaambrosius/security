package App.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Tracker {
    static java.util.logging.Logger logger = Logger.getLogger(Tracker.class.getName());

    public static void main(String[] args) throws IOException {
        int serverPort = 12345;

        ServerSocket serverSocket = new ServerSocket(serverPort);
        logger.log(Level.INFO, "P2P Server is listening on port " + serverPort);

        // Create a thread pool to handle incoming connections
        ExecutorService executor = Executors.newFixedThreadPool(10);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            // Create a new thread to handle the connection
            Runnable connectionHandler = new TrackerConnectionHandler(clientSocket);
            executor.execute(connectionHandler);
        }
    }
}
