package App.Client;

public class ConnectionC {
    public static void main(String[] args) {
        String serverPort = "12345";
        String serverIP = "localhost";
        String localPort = "54323";
        Peer user = new Peer("C", serverIP, serverPort, localPort);
        user.announceToServer();
        user.connectToPeer("A");
    }
}
