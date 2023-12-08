package App.UI;

import App.Client.Peer;
import App.Client.PeerConnection;
import App.Messages.*;
import App.Messages.Message;
import App.Storage.*;
import Utils.MessageListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;

public class MainScreenUI extends JFrame implements MessageListener {
    private JFrame frame;
    private JPanel cardPanel;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JList<String> conversationsList;
    private JButton startConversationButton;
    private final List<ConversationViewUI> activeConversations;
    private final Peer user;
    private DefaultListModel<String> currentModel = null;
    private ConversationViewUI openedChat = null;
    private GroupChatViewUI openedGroup = null;
    private final HashSet<String> existingGroupChats = new HashSet<>();
    private final HashMap<String, ArrayList<String>> groupChatsMembers = new HashMap<>();

    public MainScreenUI(Peer user) {
        this.user = user;
        initialize();
        MessagesRepository.mr();
        activeConversations = new ArrayList<>();
        user.setMessageListener(this);
        setVisible(true);
    }

    private void initialize() {
        frame = new JFrame("Secure Chat. Logged in as " + user.username);
        frame.setSize(400, 400);
        centerFrameOnScreen(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        mainPanel = new JPanel();

        mainPanel.setLayout(new BorderLayout());

        //TODO delete test users
        //conversationsList = new JList<>(new String[]{"Add User","Create group chat", "D", "Username 2", "Username 3", "Username 4", "Username 41", "Username 42", "Username 43", "Username 44", "Username 45", "Username 46", "Username 47", "Username 48", "Username 49", "Username 411"});
        getContactsList();
        conversationsList.setCellRenderer(new UnreadMessagesCellRenderer());
        if (currentModel != null) {
            conversationsList.setModel(currentModel);
        }
        mainPanel.add(new JScrollPane(conversationsList), BorderLayout.CENTER);

        startConversationButton = new JButton("Select");

        actionListener();

        mainPanel.add(startConversationButton, BorderLayout.SOUTH);


        cardPanel.add(mainPanel, "mainPanel");
        frame.getContentPane().add(cardPanel);
    }

    private void getContactsList() {
        ArrayList<String> contacts = new ArrayList<>();
        contacts.add("Add User");
        contacts.add("Create group chat");

        ArrayList<String> chatRooms = new ArrayList<>();

        for (Map.Entry<String, ChatRecord> entry : MessagesRepository.mr().peerToChat.entrySet()) {
            String peer = entry.getKey();
            contacts.add(peer);
            chatRooms.add(peer);
        }

        for (Map.Entry<String, GroupRecord> entry : MessagesRepository.mr().groups.entrySet()) {
            String group = entry.getKey();
            contacts.add(group);
            existingGroupChats.add(group);
            groupChatsMembers.put(group, entry.getValue().members);
        }

        String[] contactsArray = contacts.toArray(new String[0]);
        conversationsList = new JList<>(contactsArray);
    }

    public static void centerFrameOnScreen(JFrame frame) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;

        int frameWidth = frame.getWidth();
        int frameHeight = frame.getHeight();

        int x = (screenWidth - frameWidth) / 2;
        int y = (screenHeight - frameHeight) / 2;

        frame.setLocation(x, y);
    }

    public void actionListener() {
        startConversationButton.addActionListener(e -> {
            String selectedRecipient = conversationsList.getSelectedValue();
            if (selectedRecipient == null || selectedRecipient.equals("Add User")) {
                showNewUserPanel();
            } else if (existingGroupChats.contains(selectedRecipient)) {
                openGroupChat(selectedRecipient);
            } else if (selectedRecipient.equals("Create group chat")) {
                ArrayList<String> membersList = new ArrayList<>();
                for (int i = 0; i < conversationsList.getModel().getSize(); i++) {
                    if(!groupChatsMembers.containsKey(conversationsList.getModel().getElementAt(i))){
                        membersList.add(conversationsList.getModel().getElementAt(i));
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    new SelectGroupChatMembers(this, membersList);
                });
            } else {
                openConversationView(selectedRecipient);
            }
        });
    }

    public void createGroupChat(String groupName, ArrayList<String> members) {
        if (invalidGroupName(groupName)) {
            showWarning("Group name is equal to another name in contact list");
            return;
        }
        addNewContactOrGroup(groupName);
        existingGroupChats.add(groupName);
        members.add(user.username);
        groupChatsMembers.put(groupName, members);

        try {
            GroupRecord newGroup = new GroupRecord(members, groupName);
            MessagesRepository.mr().addGroup(newGroup);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        GroupChatViewUI creatingGroup = new GroupChatViewUI(members, user, groupName);

    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(cardPanel, message);
    }


    private boolean invalidGroupName(String groupName) {
        if(groupChatsMembers.containsKey(groupName)) {
            return true;
        }
        for (int i = 0; i < conversationsList.getModel().getSize(); i++) {
            if(conversationsList.getModel().getElementAt(i).equals(groupName)) {
                return true;
            }
        }
        return false;
    }


    //Used when creating a new group chat
    public void addGroupChat(String groupName, ArrayList<String> members, String groupStorageKey) {
        //TODO check if group name is not equal to any user name or any group

        addNewContactOrGroup(groupName);
        existingGroupChats.add(groupName);
        groupChatsMembers.put(groupName, members);
        MessagesRepository.mr().addGroup(new GroupRecord(members, groupName, groupStorageKey));
    }

    //Used when received a group chat invitation
    private void openGroupChat(String groupName) {
        GroupChatViewUI groupChatViewUI = new GroupChatViewUI(this, groupChatsMembers.get(groupName), currentModel, this.user, groupName);
        groupChatViewUI.setVisible(true);
        openedGroup = groupChatViewUI;
        UnreadMessagesCellRenderer.readMessage(groupName);
        setVisible(false);
    }

    private void openConversationView(String user) {
        ConversationViewUI conversationView = new ConversationViewUI(this, user, this.user);
        activeConversations.add(conversationView);
        conversationView.setVisible(true);
        openedChat = conversationView;
        UnreadMessagesCellRenderer.readMessage(user);
        setVisible(false);
    }

    private void showNewUserPanel() {
        JPanel newPanel = new JPanel();
        JTextField textField = new JTextField(20);
        JButton submitButton = new JButton("Submit");
        JButton backButton = new JButton("Back");

        submitButton.addActionListener(e -> newUser(textField));
        backButton.addActionListener(e -> cardPanel.remove(cardPanel.getComponentCount() - 1));

        newPanel.add(new JLabel("Enter User Name: "));
        newPanel.add(textField);
        newPanel.add(submitButton);
        newPanel.add(backButton);

        textField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    newUser(textField);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        cardPanel.add(newPanel, "newPanel");
        cardLayout.show(cardPanel, "newPanel");
    }


    public void newUser(JTextField textField) {
        String inputText = textField.getText();
        if (!checkNewUser(inputText) || !isNewUser(inputText)) {
            cardPanel.remove(cardPanel.getComponentCount() - 1);
            showWarning("User not found!");
            return; //TODO handle error trying to find user in server
        }
        cardPanel.remove(cardPanel.getComponentCount() - 1);
        addNewContactOrGroup(inputText);
    }

    /**
     * Checks if user already exists in the UI users list
     **/
    private Boolean isNewUser(String username) {
        for (int i = 0; i < conversationsList.getModel().getSize(); i++) {
            if (username.equalsIgnoreCase(conversationsList.getModel().getElementAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void addNewContactOrGroup(String name) {
        if (currentModel != null) {
            currentModel.addElement(name);
            conversationsList.setModel(currentModel);
            SwingUtilities.invokeLater(() -> {
                conversationsList.setModel(currentModel);
                mainPanel.revalidate();
                mainPanel.repaint();
            });
            return;
        }
        DefaultListModel<String> newModel = new DefaultListModel<>();

        for (int i = 0; i < conversationsList.getModel().getSize(); i++) {
            newModel.addElement(conversationsList.getModel().getElementAt(i));
        }

        newModel.addElement(name);
        currentModel = newModel;

        conversationsList.setModel(currentModel);
        SwingUtilities.invokeLater(() -> {
            conversationsList.setModel(currentModel);
            mainPanel.revalidate();
            mainPanel.repaint();
        });
    }

    private Boolean checkNewUser(String username) {
        return user.getPeerData(username) != null;
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public void closeChat() {
        openedChat = null;
    }

    public void closeGroup() {
        openedGroup = null;
    }

    public JFrame getFrame() {
        return frame;
    }

    @Override
    public void messageReceived(Message message, String peerUsername) {
        MessageType messageType = message.getType();
        switch (messageType) {
            case GROUP_INVITATION -> handleGroupMessageInvitation((GroupInvitationMessage) message, peerUsername);
            case GROUP_MESSAGE -> handleGroupMessage((GroupMessage) message, peerUsername);
            case MESSAGE -> handleNormalChatMessages((ChatMessage) message, peerUsername);
        }
    }

    /**
     * Listens everytime a users disconnects and proceeds to remove the connection
     **/
    @Override
    public void connectionEnded(PeerConnection peerConnection) {
        List<PeerConnection> peerConnectionsValues = new ArrayList<>(user.peerConnections.values());
        List<String> peerConnectionsUsers = new ArrayList<>(user.peerConnections.keySet());
        String tropa = "";
        for (int i = 0; i < peerConnectionsValues.size(); i++) {
            if (peerConnectionsValues.get(i).equals(peerConnection)) {
                user.peerConnections.remove(peerConnectionsUsers.get(i));
                tropa = peerConnectionsUsers.get(i);
            }
        }
    }

    @Override
    public void userOffline(String user) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (openedChat != null) {
            openedChat.close();
            showWarning(user + " is offline!");
        }
        if (openedGroup != null) {
            openedGroup.close();
            //showWarning(user + " is offline!");
        }
    }


    private void handleNormalChatMessages(ChatMessage messageToStore, String peerUsername) {
        String chatId = user.peerConnections.get(peerUsername).chatId;
        StorageMessage storageMessage = new StorageMessage(messageToStore, peerUsername, chatId);
        MessagesRepository.mr().addMessage(storageMessage);

        if (isChatActive(peerUsername)) {
            // Send the msg to the open chat window to be displayed
            openedChat.showMessageReceived(messageToStore.getMessageText(), peerUsername);
        } else {
            // Changes the user tile to another color, because of unread message
            UnreadMessagesCellRenderer.unreadMessage(peerUsername);
            SwingUtilities.invokeLater(() -> {
                mainPanel.revalidate();
                mainPanel.repaint();
            });
        }
    }

    private boolean isChatActive(String peer) {
        return openedChat != null && openedChat.getReceiverUsername().equals(peer);
    }

    private void handleGroupMessageInvitation(GroupInvitationMessage message, String sender) {
        String groupName = message.getGroupName();
        String storageKey = message.getStorageKey();
        ArrayList<String> members = message.getMembers();
        checkInvitation(sender, groupName, members, storageKey);
    }

    private void handleGroupMessage(GroupMessage message, String sender) {
        String groupName = message.getGroupName();
        StorageMessage storageMessage = new StorageMessage(message, sender);
        MessagesRepository.mr().addMessage(storageMessage);
        handleMessagesFromGroupChats(groupName, storageMessage);
    }

    private void handleMessagesFromGroupChats(String groupName, StorageMessage message) {
        if (isGroupWindowOpened(groupName)) {
            openedGroup.showMessageReceived(message);
        } else {
            UnreadMessagesCellRenderer.unreadMessage(groupName);
            SwingUtilities.invokeLater(() -> {
                mainPanel.revalidate();
                mainPanel.repaint();
            });
        }
    }

    private boolean isGroupWindowOpened(String groupName) {
        return openedGroup != null && openedGroup.getGroupName().equals(groupName);
    }

    private void checkInvitation(String host, String groupName, ArrayList<String> members, String groupStorageKey) {
        if (isGroupHostKnown(host)) {
            // Only someone from contacts can add peer in a new group
            addGroupChat(groupName, members, groupStorageKey);
        }
    }

    public boolean isGroupHostKnown(String host) {
        ListModel<String> model = conversationsList.getModel();
        for (int i = 0; i < model.getSize(); i++)
            if (model.getElementAt(i).equals(host)) return true;
        return false;
    }


    public void placeFrameInCoordinates(int x, int y) {
        frame.setLocation(x, y);
    }

    private static class UnreadMessagesCellRenderer extends DefaultListCellRenderer {

        private static HashMap<String, Boolean> unreadMessages = new HashMap<>();

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (unreadMessages.containsKey(value.toString())) {
                Boolean unread = unreadMessages.get(value.toString());
                if (unread) {
                    c.setBackground(Color.RED);
                }
            }
            return c;
        }

        public static void unreadMessage(String senderUsername) {
            unreadMessages.put(senderUsername, true);
        }
        public static void readMessage(String senderUsername) {
            unreadMessages.put(senderUsername, false);
        }
    }
}


