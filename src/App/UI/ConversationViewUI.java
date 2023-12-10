package App.UI;

import App.Client.Peer;
import App.Client.PeerConnection;
import App.Messages.ChatMessage;
import App.Storage.MessagesRepository;
import App.Storage.StorageMessage;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ConversationViewUI implements MessageObserver {
    private static final Logger logger = Logger.getLogger(ConversationViewUI.class.getName());

    private JFrame frame;
    private JTextArea messageDisplayArea;
    private JTextField messageInputField;
    private JButton sendButton;
    private JButton searchButton;
    private JButton backButton;
    private final String receiverUsername;
    private final Peer user;
    private final MainScreenUI mainUI;

    private boolean subscribeLater = false;

    public ConversationViewUI(MainScreenUI mainUI, String recipientName, Peer user, @Nullable Integer x, @Nullable Integer y) {
        this.mainUI = mainUI;
        this.receiverUsername = recipientName;
        this.user = user;
        if (!user.peerConnections.containsKey(receiverUsername)){
            new Thread(() -> user.connectToPeer(receiverUsername)).start();
        }
        initialize(recipientName,x,y);
    }

    private void initialize(String recipientName, Integer x, Integer y) {
        frame = new JFrame("Conversation with " + recipientName);
        frame.setSize(400, 400);

        if (x==null){
            frame.setLocation(mainUI.getFrame().getX(), mainUI.getFrame().getY());
        } else {
            frame.setLocation(x, y);
        }
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        messageDisplayArea = new JTextArea();
        messageDisplayArea.setEditable(false);
        String chatId = MessagesRepository.mr().getChatId(recipientName);
        if(chatId == null) {
            subscribeLater = true;
        }
        MessagesRepository.mr().subscribe(this, chatId);
        for (StorageMessage m : MessagesRepository.mr().getChatHistory(chatId)) updateMessage(m);

        frame.getContentPane().add(new JScrollPane(messageDisplayArea), BorderLayout.CENTER);

        messageInputField = new JTextField();
        frame.getContentPane().add(messageInputField, BorderLayout.SOUTH);

        sendButton = new JButton("Send");
        backButton = new JButton("Back");
        searchButton = new JButton("Search");

        actionlistener();
        keyListener();

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(backButton);
        buttonPanel.add(searchButton);

        frame.add(buttonPanel,BorderLayout.NORTH);

        frame.getContentPane().add(sendButton, BorderLayout.EAST);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                user.sendMessagesToRemoteServer(chatId);
                frame.setVisible(false);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // do nothing
                }
                frame.dispose();
                System.exit(0);
            }
        });
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
            String chatId = MessagesRepository.mr().getChatId(receiverUsername);
            user.sendMessagesToRemoteServer(chatId);
            mainUI.closeChat();
            frame.dispose();
            mainUI.placeFrameInCoordinates(frame.getX(),frame.getY());
            mainUI.setVisible(true);
        });
        searchButton.addActionListener(e -> {
            String chatId = MessagesRepository.mr().getChatId(receiverUsername);
            user.sendMessagesToRemoteServer(chatId);
            mainUI.closeChat();
            frame.dispose();
            SearchChatUI searchChatUI = new SearchChatUI(frame,mainUI,receiverUsername, user);
        });
    }

    public void close() {
        mainUI.closeChat();
        frame.dispose();
        mainUI.placeFrameInCoordinates(frame.getX(),frame.getY());
        mainUI.setVisible(true);
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public void sendMessage() {
        String plaintext = messageInputField.getText();
        messageInputField.setText("");

        logger.log(Level.INFO, "Sending " + plaintext + " to " + receiverUsername);
        PeerConnection receiver = user.peerConnections.get(receiverUsername);

        if (plaintext.isEmpty()) return;

        try {
            String signature = user.encryptionManager.signMessage(plaintext);
            ChatMessage message = new ChatMessage(signature, plaintext);
            String chatId = MessagesRepository.mr().getChatId(receiverUsername);
            if(subscribeLater) {
                MessagesRepository.mr().subscribe(this, chatId);
            }
            StorageMessage messageToStore = new StorageMessage(message, user.username, chatId);
            MessagesRepository.mr().addMessage(messageToStore);

            new Thread(() -> receiver.sendMessage(message.encode())).start();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            logger.log(Level.WARNING, "Message not send");
        }
    }

    public void showMessageReceived(String message, String peer) {
        if (peer.equals(receiverUsername)) {
            messageInputField.setText("");
        }
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    @Override
    public void updateMessage(StorageMessage m) {
        messageDisplayArea.append(m.sender + ": " + m.message + "\n");
    }

    @Override
    public void updateAll(ArrayList<StorageMessage> mList) {
        messageDisplayArea.setText("");
        for (StorageMessage m : mList) messageDisplayArea.append(m.sender + ": " + m.message + "\n");
    }
}