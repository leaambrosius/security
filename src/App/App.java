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
    }
    // TODO handle nacks

    public static void runMainUI(String username) {
        try {
            Peer user = new Peer(username, serverIP, serverPort, localPort);
            MainScreenUI mainScreen = new MainScreenUI(user);
        } catch (Exception e) {
            System.exit(1);
        }
    }
}
