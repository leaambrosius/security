package App;

import App.Client.Peer;
import App.UI.MainScreenUI;
import App.UI.UsernameSubmitUI;

import javax.swing.*;

public class App {
    static String serverPort = "12345";
    static String serverIP = "localhost";
    static String localPort = "54321";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UsernameSubmitUI::new);

        /*Peer user = new Peer("A", serverIP, serverPort, localPort);
        Peer user = new Peer(username, serverIP, serverPort, localPort);
        user.announceToServer();
        MainScreenUI mainScreen = new MainScreenUI(user);*/

    }

    public static void runMainUI(String username) {
        Peer user = new Peer(username, serverIP, serverPort, localPort);
        user.announceToServer();
        MainScreenUI mainScreen = new MainScreenUI(user);
    }

}
