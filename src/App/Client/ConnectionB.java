package App.Client;

public class ConnectionB {
    public static void main(String[] args) {
        String serverPort = "12345";
        String serverIP = "localhost";
        String localPort = "54322";
        Peer user = new Peer("B", serverIP, serverPort, localPort);
        user.announceToServer();
        //user.connectToPeer("A");
    }
}
