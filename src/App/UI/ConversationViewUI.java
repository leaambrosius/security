package App.UI;

import App.Client.Peer;
import App.Client.PeerConnection;
import App.Messages.ChatMessage;
import App.Storage.Message;
import App.Storage.MessagesRepository;
import App.Storage.StorageMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ConversationViewUI {
    private static final Logger logger = Logger.getLogger(ConversationViewUI.class.getName());

    private JFrame frame;
    private JTextArea messageDisplayArea;
    private JTextField messageInputField;
    private JButton sendButton;
    private JButton backButton;
    private MessagesRepository messageRepository;
    private String receiverUsername;
    private DefaultListModel<String> mainModel;
    private Peer user;
    private MainScreenUI mainUI;


    public ConversationViewUI(MainScreenUI mainUI, String recipientName, DefaultListModel<String> mainModel, Peer user,MessagesRepository messageRepository) {

        this.mainUI = mainUI;
        this.receiverUsername = recipientName;
        this.mainModel = mainModel;
        this.user = user;
        this.messageRepository = messageRepository;
        if (!user.peerConnections.containsKey(receiverUsername)){
            new Thread(() -> user.connectToPeer(receiverUsername)).start();
        }
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

        String chatId = messageRepository.peerToChat.get(recipientName).chatId;
        for (StorageMessage msg : messageRepository.getChatHistory(chatId)) {
            messageDisplayArea.append(msg.sender + ": " + msg.message+"\n");
        }

        frame.getContentPane().add(new JScrollPane(messageDisplayArea), BorderLayout.CENTER);

        messageInputField = new JTextField();
        frame.getContentPane().add(messageInputField, BorderLayout.SOUTH);

        sendButton = new JButton("Send");

        backButton = new JButton("Back");

        actionlistener();
        keyListener();

        frame.getContentPane().add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(backButton, BorderLayout.NORTH);
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

    public void close() {
        mainUI.closeChat();
        frame.dispose();
        mainUI.placeFrameInCoordinates(frame.getX(),frame.getY());
        mainUI.setVisible(true);
    }

    public void appendUnreadMessages() {
//        if(mainUI.getUnreadMessages().containsKey(receiverUsername)){
//            //TODO use this if local/remote storage is not working
//            //This is needed to show messages unread if we dont have any kind of storage implemented
//            /*ArrayList<Message> messages = mainUI.getUnreadMessages().get(receiverUsername);
//            for (int i = 0; i < messages.size(); i++) {
//                String before = messageDisplayArea.getText();
//                String msgText = messages.get(i).plaintext;
//                messageDisplayArea.append(receiverUsername + ": " + messages.get(i).plaintext + "\n");
//                String after = messageDisplayArea.getText();
//            }*/
//            mainUI.deleteStoredUnreadMessages(receiverUsername);
//        }
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public void sendMessage() {
        String plaintext = messageInputField.getText();
        PeerConnection receiver = user.peerConnections.get(receiverUsername);

        if (plaintext.isEmpty()) return;

        try {
            String signature = user.encryptionManager.signMessage(plaintext);
            ChatMessage message = new ChatMessage(signature, plaintext);
            new Thread(() -> receiver.sendMessage(message.encode())).start();

            messageDisplayArea.append("Me: " + plaintext + "\n");
            messageInputField.setText("");

            String chatId = messageRepository.peerToChat.get(receiverUsername).chatId;
            StorageMessage messageToStore = new StorageMessage(message, user.username, chatId);
            messageRepository.addMessage(messageToStore, chatId);

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            logger.log(Level.WARNING, "Message not send");
            return;
        }
    }

    public void showMessageReceived(String message, String peer) {
        if (peer.equals(receiverUsername)) {
            messageDisplayArea.append(receiverUsername + ": " + message + "\n");
            messageInputField.setText("");
        }
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }
}