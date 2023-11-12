package App.Client;

public class Connection {

    public static void main(String[] args) {
        Peer user = new Peer("ala");
        user.announceToServer();
        user.connectToPeer("ola");
    }
}
