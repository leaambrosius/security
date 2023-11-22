package App;

import App.Client.Peer;
import App.UI.MainScreenUI;

public class App {
    public static void main(String[] args) {

        String serverPort = "12345";
        String serverIP = "localhost";
        String localPort = "54321";
        Peer user = new Peer("A", serverIP, serverPort, localPort);
        user.announceToServer();
        MainScreenUI mainScreen = new MainScreenUI(user);

    }

}
