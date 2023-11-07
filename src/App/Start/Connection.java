package App.Start;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

public class Connection {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        String ip = getIp();
        Client user = new Client(ip);
        String serverAddress = "localhost";//"192.168.1.122"; // Replace with the server's IP address or hostname
        int serverPort = 12345; // Replace with the server's port

        try (Socket socket = new Socket(serverAddress, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            System.out.println("Connected to the P2P server.");

            // Send a message to the server
            user.registOrLogin(out);
            //user.getUserAddressFromTracker(out,"Geremilo");

            // Wait for the server's response
            String response = in.readLine();
            if (response != null) {
                System.out.println("Server response: " + response);
                HandleResponseFromTracker(response,user);
            } else {
                System.out.println("No response from the server.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getIp() {
        try {
            URL url = new URL("https://api64.ipify.org?format=text");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String publicIP = reader.readLine();
            reader.close();
            return publicIP;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void HandleResponseFromTracker(String response, Client user) {
        if(response.equals("User already registered")){
            user.deleteFile();
        }
    }


}
