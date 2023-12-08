package App.UI;

import App.Client.Peer;
import App.Client.PeerConnection;
import App.Messages.GroupInvitationMessage;
import App.Messages.GroupMessage;
import App.Storage.MessagesRepository;
import App.Storage.StorageMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GroupChatViewUI {
    private static final Logger logger = Logger.getLogger(GroupChatViewUI.class.getName());


    private JFrame frame;
    private JTextArea messageDisplayArea;

    private JTextField messageInputField;
    private JButton sendButton;
    private JButton backButton;

    private ArrayList<String> members;
    private MessagesRepository messageRepository;
    private DefaultListModel<String> mainModel;

    private String groupName;

    private Peer user;

    private MainScreenUI mainUI;


    public GroupChatViewUI(MainScreenUI mainUI, ArrayList<String> members, DefaultListModel<String> mainModel, Peer user, String groupName, MessagesRepository messageRepository) {
        this.mainUI = mainUI;
        this.members = members;
        this.mainModel = mainModel;
        this.user = user;
        this.groupName = groupName;
        this.messageRepository = messageRepository;
        openSocketsWithGroupMembers();
        initialize();
    }

    public GroupChatViewUI(ArrayList<String> members, Peer user, String groupName, MessagesRepository messageRepository)

    {
        this.members = members;
        this.user = user;
        this.groupName = groupName;
        this.messageRepository = messageRepository;

        openSocketsWithGroupMembers();
        //TODO find a better way to wait for handshake and then invite members
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        inviteMembers();
    }

    public void inviteMembers() {
        GroupInvitationMessage groupInvitationMessage = new GroupInvitationMessage(groupName, members.toString());
        for (String member : members) {
            PeerConnection receiver = user.peerConnections.get(member);
            if (receiver != null) {
                // TODO do we need new thread for each?
                new Thread(() -> {
                    receiver.sendMessage(groupInvitationMessage.encode());
                }).start();
            }
        }
    }

    private void openSocketsWithGroupMembers() {
        for (String receiverUsername : members) {
            if (!user.peerConnections.containsKey(receiverUsername) && !user.username.equals(receiverUsername)) {
                new Thread(() -> user.connectToPeer(receiverUsername)).start();
            }
        }
    }

    private StringBuilder groupChatDisplayMessage() {
        StringBuilder membersDisplayText = new StringBuilder();
        for (String member : members) {
            membersDisplayText.append(member).append(", ");
        }
        return membersDisplayText;
    }

    private void initialize() {
        //TODO change the this to group name and maybe add a option to see group members
        frame = new JFrame("Group with " + groupChatDisplayMessage());


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

        for (StorageMessage message : messageRepository.getChatHistory(groupName)) {
            messageDisplayArea.append(message.sender + ": " + message.message+"\n");
        }
    }

    public void keyListener() {
        messageInputField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) { }
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
            @Override
            public void keyReleased(KeyEvent e) { }
        });

    }

    public void actionlistener() {
        sendButton.addActionListener(e -> sendMessage());
        backButton.addActionListener(e -> {
            mainUI.closeGroup();
            frame.dispose();
            mainUI.placeFrameInCoordinates(frame.getX(), frame.getY());
            mainUI.setVisible(true);
        });

    }

    public void close() {
        mainUI.closeGroup();
        frame.dispose();
        mainUI.placeFrameInCoordinates(frame.getX(), frame.getY());
        mainUI.setVisible(true);
    }

//    public void appendUnreadMessages() {
//        if(mainUI.getUnreadMessages().containsKey(groupName)){
//            ArrayList<StorageMessage> messages = mainUI.getUnreadMessages().get(groupName);
//            for (StorageMessage message : messages) {
//                messageDisplayArea.append(message.sender + ": " + message.message + "\n");
//            }
//            mainUI.deleteStoredUnreadMessages(groupName);
//        }
//    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public void sendMessage() {
        String plaintext = messageInputField.getText();

        if (plaintext.isEmpty()) return;

        messageDisplayArea.append("Me: " + plaintext + "\n");
        messageInputField.setText("");

        try {
            String signature = user.encryptionManager.signMessage(plaintext);
            GroupMessage groupMessage = new GroupMessage(signature, plaintext, groupName);

            for (String member : members) {
                PeerConnection receiver = user.peerConnections.get(member);
                if (receiver != null) {
                    new Thread(() -> {
                        receiver.sendMessage(groupMessage.encode());
                    }).start();
                }
            }

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            logger.log(Level.WARNING, "Group message not send");
        }
    }

    public void showMessageReceived(StorageMessage message) {
        messageDisplayArea.append(message.sender + ": " + message.message + "\n");
        messageInputField.setText("");
    }

    public String getGroupName() {
        return this.groupName;
    }
}