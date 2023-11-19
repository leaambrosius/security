package App.Client;

public class Connection {
    public static void main(String[] args) {
        Peer user = new Peer("A");
        user.announceToServer();
//        user.connectToPeer("ola");
    }
}
