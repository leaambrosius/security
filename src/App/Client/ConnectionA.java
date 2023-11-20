package App.Client;

public class ConnectionA {
    public static void main(String[] args) {
        String serverPort = "12345";
        String serverIP = "localhost";
        String localPort = "54321";
        Peer user = new Peer("A", serverIP, serverPort, localPort);
        user.announceToServer();

//        user.connectToPeer("ola");
    }
}
