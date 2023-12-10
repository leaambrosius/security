package App;

import App.Client.Peer;
import App.Storage.FileManager;
import App.Storage.StorageMessage;
import App.UI.MainScreenUI;
import App.UI.UsernameSubmitUI;

import javax.swing.*;

public class App {
    static String serverPort = "12345";
    static String serverIP = "192.168.251.42";
    static String localPort = "54323";

    public static void main(String[] args) {
        if (args.length > 0) {
            localPort = args[0];
        }
        SwingUtilities.invokeLater(UsernameSubmitUI::new);
    }
    // TODO handle nacks

    public static void runMainUI(String username) {
        try {
            Peer user = new Peer(username, serverIP, serverPort, localPort);
            FileManager.setUsername(username);
            MainScreenUI mainScreen = new MainScreenUI(user);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
