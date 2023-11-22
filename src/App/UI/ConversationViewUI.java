package App.UI;

import App.App;
import App.Client.Peer;
import App.Client.PeerConnection;
import App.Storage.Message;
import Utils.MessageListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;


public class ConversationViewUI {
    private JFrame frame;
    private JTextArea messageDisplayArea;

    private JTextField messageInputField;
    private JButton sendButton;
    private JButton backButton;

    private String receiverUsername;
    private DefaultListModel<String> mainModel;

    private Peer user;

    private MainScreenUI mainUI;


    public ConversationViewUI(MainScreenUI mainUI, String recipientName, DefaultListModel<String> mainModel, Peer user) {
        this.mainUI = mainUI;
        this.receiverUsername = recipientName;
        this.mainModel = mainModel;
        this.user = user;
        new Thread(() -> user.connectToPeer(receiverUsername)).start();
        initialize(recipientName);
    }

    private void initialize(String recipientName) {
        frame = new JFrame("Conversation with " + recipientName);


        frame.setSize(400, 400);
        //MainScreenUI.centerFrameOnScreen(frame);
        frame.setLocation(mainUI.getFrame().getX(), mainUI.getFrame().getY());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        messageDisplayArea = new JTextArea();
        messageDisplayArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(messageDisplayArea), BorderLayout.CENTER);

        messageInputField = new JTextField();
        frame.getContentPane().add(messageInputField, BorderLayout.SOUTH);

        sendButton = new JButton("Send");

        backButton = new JButton("Back");

        actionlistener();
        keyListener();

        frame.getContentPane().add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(backButton, BorderLayout.NORTH);

        appendUnreadMessages();
    }

    public void keyListener() {
        messageInputField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

    }

    public void actionlistener() {

        sendButton.addActionListener(e -> sendMessage());

        backButton.addActionListener(e -> {
            mainUI.closeChat();
            frame.dispose();
            mainUI.placeFrameInCoordinates(frame.getX(),frame.getY());
            mainUI.setVisible(true);
        });

    }

    public void appendUnreadMessages() {
        if(mainUI.getUnreadMessages().containsKey(receiverUsername)){
            ArrayList<Message> messages = mainUI.getUnreadMessages().get(receiverUsername);
            for (int i = 0; i < messages.size(); i++) {
                messageDisplayArea.append(receiverUsername + ": " + messages.get(i).plaintext + "\n");
            }
            mainUI.deleteStoredUnreadMessages(receiverUsername);
        }
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public void sendMessage() {
        String message = messageInputField.getText();
        PeerConnection receiver = user.peerConnections.get(receiverUsername);
        new Thread(() -> {
            receiver.sendMessage(message);
        }).start();
        if (!message.isEmpty()) {
            messageDisplayArea.append("Me: " + message + "\n");
            messageInputField.setText("");
        }
    }

    public void showMessageReceived(Message message) {
        if (message.peerUsername.equals(receiverUsername)) {
            messageDisplayArea.append(receiverUsername + ": " + message.plaintext + "\n");
            messageInputField.setText("");
        }
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }
}