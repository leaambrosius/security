package App;

import App.Client.Peer;
import App.UI.MainScreenUI;
import App.UI.UsernameSubmitUI;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

public class App {
    static String serverPort = "12345";
    static String serverIP = "localhost";
    static String localPort = "54321";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UsernameSubmitUI::new);
        /*ArrayList<String> x = new ArrayList<>();
        x.add("xiu");
        x.add("tropa");
        x.add("toy");
        String fiji = x.toString();
        System.out.println(fiji);
        String cleanedFiji = fiji.replaceAll("[\\[\\]\"]", "");
        String[] array = cleanedFiji.split(",\\s*");
        ArrayList<String> k = new ArrayList<>(Arrays.asList(array));
        System.out.println(k.toString());*/

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
