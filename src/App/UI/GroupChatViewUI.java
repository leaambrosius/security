package App.UI;

import App.Client.Peer;
import App.Client.PeerConnection;
import App.Messages.GroupInvitationMessage;
import App.Messages.GroupMessage;
import App.Storage.MessagesRepository;
import App.Storage.StorageMessage;

import javax.crypto.KeyGenerator;
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
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GroupChatViewUI implements MessageObserver  {
    private static final Logger logger = Logger.getLogger(GroupChatViewUI.class.getName());


    private JFrame frame;
    private JTextArea messageDisplayArea;

    private JTextField messageInputField;
    private JButton sendButton;
    private JButton backButton;

    private ArrayList<String> members;
    private String groupName;
    private Peer user;
    private MainScreenUI mainUI;


    public GroupChatViewUI(MainScreenUI mainUI, ArrayList<String> members, DefaultListModel<String> mainModel, Peer user, String groupName) {
        this.mainUI = mainUI;
        this.members = members;
        this.user = user;
        this.groupName = groupName;
        openSocketsWithGroupMembers();
        initialize();
    }

    public GroupChatViewUI(ArrayList<String> members, Peer user, String groupName) {
        this.members = members;
        this.user = user;
        this.groupName = groupName;

        openSocketsWithGroupMembers();
        //TODO find a better way to wait for handshake and then invite members
        try {
            Thread.sleep(1000);
            inviteMembers();
        } catch (InterruptedException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void inviteMembers() throws NoSuchAlgorithmException {
        String storageKey = Base64.getEncoder().encodeToString(KeyGenerator.getInstance("AES").generateKey().getEncoded());;
        GroupInvitationMessage groupInvitationMessage = new GroupInvitationMessage(groupName, members, storageKey);
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

        MessagesRepository.mr().subscribe(this, groupName);
        for (StorageMessage message : MessagesRepository.mr().getChatHistory(groupName)) {
            messageDisplayArea.append(message.sender + ": " + message.message+"\n");
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                user.sendMessagesToRemoteServer(groupName);
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
            user.sendMessagesToRemoteServer(groupName);
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

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public void sendMessage() {
        String plaintext = messageInputField.getText();
        messageInputField.setText("");

        if (plaintext.isEmpty()) return;

        try {
            String signature = user.encryptionManager.signMessage(plaintext);
            GroupMessage groupMessage = new GroupMessage(signature, plaintext, groupName);

            for (String member : members) {
                PeerConnection receiver = user.peerConnections.get(member);
                if (receiver != null) {
                    new Thread(() -> receiver.sendMessage(groupMessage.encode())).start();
                }
            }
            StorageMessage messageToStore = new StorageMessage(groupMessage, user.username);
            MessagesRepository.mr().addMessage(messageToStore);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            logger.log(Level.WARNING, "Group message not send");
        }
    }

    public void showMessageReceived(StorageMessage message) {
        messageInputField.setText("");
    }

    public String getGroupName() {
        return this.groupName;
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