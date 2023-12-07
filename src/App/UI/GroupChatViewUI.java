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


public class GroupChatViewUI {
    private JFrame frame;
    private JTextArea messageDisplayArea;

    private JTextField messageInputField;
    private JButton sendButton;
    private JButton backButton;

    private ArrayList<String> members;
    private DefaultListModel<String> mainModel;

    private String groupName;

    private Peer user;

    private MainScreenUI mainUI;


    public GroupChatViewUI(MainScreenUI mainUI, ArrayList<String> members, DefaultListModel<String> mainModel, Peer user,String groupName) {
        this.mainUI = mainUI;
        this.members = members;
        this.mainModel = mainModel;
        this.user = user;
        this.groupName = groupName;
        openSocketsWithGroupMembers();
        initialize();
    }

    public GroupChatViewUI(ArrayList<String> members, Peer user,String groupName)

    {
        this.members = members;
        this.user = user;
        this.groupName = groupName;
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
        //TODO this is probably dumb

        //messageInvitationIdentifier @ groupName @ members
        String groupMessageInvitationIdentifier = "GROUP_INVITATION@"+groupName+"@"+members.toString();
        for (int i = 0; i < members.size(); i++) {
            PeerConnection receiver = user.peerConnections.get(members.get(i));
            if(receiver != null) {
                new Thread(() -> {
                    receiver.sendMessage(groupMessageInvitationIdentifier);
                }).start();
            }
        }
    }

    private void openSocketsWithGroupMembers() {
        for (int i = 0; i < members.size(); i++) {
            String receiverUsername = members.get(i);
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

    public void appendUnreadMessages() {
        if(mainUI.getUnreadMessages().containsKey(groupName)){
            ArrayList<Message> messages = mainUI.getUnreadMessages().get(groupName);
            for (int i = 0; i < messages.size(); i++) {
                messageDisplayArea.append(messages.get(i).peerUsername + ": " + messages.get(i).plaintext + "\n");
            }
            mainUI.deleteStoredUnreadMessages(groupName);
        }
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public void sendMessage() {
        //TODO this is probably dumb
        String groupMessageIdentifier = "GROUP_MESSAGE@"+groupName+"@";
        String message = messageInputField.getText();
        for (int i = 0; i < members.size(); i++) {
            PeerConnection receiver = user.peerConnections.get(members.get(i));
            if (receiver != null) {
                new Thread(() -> {
                    receiver.sendMessage(groupMessageIdentifier + message);
                }).start();
            }
        }

        if (!message.isEmpty()) {
            messageDisplayArea.append("Me: " + message + "\n");
            messageInputField.setText("");
        }
    }

    public void showMessageReceived(Message message) {
        messageDisplayArea.append(message.peerUsername + ": " + message.plaintext + "\n");
        messageInputField.setText("");
    }

    public String getGroupName() {
        return this.groupName;
    }

    /*public String getReceiverUsername() {
        return receiverUsername;
    }*/
}