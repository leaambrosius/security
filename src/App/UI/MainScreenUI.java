package App.UI;

import App.Client.Peer;
import App.Client.PeerConnection;
import App.Storage.Message;
import Utils.MessageListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.List;

public class MainScreenUI extends JFrame implements MessageListener {
    private JFrame frame;
    private JPanel cardPanel;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JList<String> conversationsList;
    private JButton startConversationButton;
    private List<ConversationViewUI> activeConversations;
    private Peer user;
    private DefaultListModel<String> currentModel = null;
    private ConversationViewUI openedChat = null;
    private GroupChatViewUI openedGroup = null;

    private HashSet<String> existingGroupChats = new HashSet<>();
    private HashMap<String,ArrayList<String>> groupChatsMembers = new HashMap<>();

    private HashMap<String, ArrayList<Message>> unreadMessages = new HashMap<>();

    public MainScreenUI(Peer user, DefaultListModel<String> mainModel) {
        this.user = user;
        this.currentModel = mainModel;
        initialize();
        activeConversations = new ArrayList<>();
        setVisible(true);
    }

    public MainScreenUI(Peer user) {
        this.user = user;
        initialize();
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
        conversationsList = new JList<>(new String[]{"Add User","Create group chat", "Username 1", "Username 2", "Username 3", "Username 4", "Username 41", "Username 42", "Username 43", "Username 44", "Username 45", "Username 46", "Username 47", "Username 48", "Username 49", "Username 411"});
        conversationsList.setCellRenderer(new UnreadMessagesCellRenderer());
        if (currentModel != null) {
            conversationsList.setModel(currentModel);
        }
        mainPanel.add(new JScrollPane(conversationsList), BorderLayout.CENTER);

        startConversationButton = new JButton("Start Conversation");

        actionListener();

        mainPanel.add(startConversationButton, BorderLayout.SOUTH);


        cardPanel.add(mainPanel, "mainPanel");
        frame.getContentPane().add(cardPanel);
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
                SwingUtilities.invokeLater(() -> {
                    new SelectGroupChatMembers(this,conversationsList);
                });
            } else {
                openConversationView(selectedRecipient);
            }
        });
    }

    public void createGroupChat(String groupName, ArrayList<String> members) {
        //TODO check if group name is not equal to any user name or any group

        addNewContactOrGroup(groupName);
        existingGroupChats.add(groupName);
        members.add(user.username);
        groupChatsMembers.put(groupName,members);
        GroupChatViewUI creatingGroup = new GroupChatViewUI(members,user,groupName);
    }

    //Used when creating a new group chat
    public void addGroupChat(String groupName, ArrayList<String> members) {
        //TODO check if group name is not equal to any user name or any group

        addNewContactOrGroup(groupName);
        existingGroupChats.add(groupName);
        groupChatsMembers.put(groupName,members);
    }

    //Used when received a group chat invitation
    private void openGroupChat(String groupName) {
        GroupChatViewUI groupChatViewUI = new GroupChatViewUI(this,groupChatsMembers.get(groupName),currentModel,this.user,groupName);
        groupChatViewUI.setVisible(true);
        openedGroup = groupChatViewUI;
        UnreadMessagesCellRenderer.readMessage(groupName);
        setVisible(false);
    }

    private void openConversationView(String user) {
        ConversationViewUI conversationView = new ConversationViewUI(this, user, currentModel, this.user);
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
            JOptionPane.showMessageDialog(cardPanel, "User not found!");
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
        String serverAddress = "127.0.0.1";
        int serverPort = 12345;
        try (Socket socket = new Socket(serverAddress, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            System.out.println("Connected to the P2P server.");

            if (user.getPeerData(username) != null) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public void closeChat() {
        openedChat = null;
    }

    public JFrame getFrame() {
        return frame;
    }

    /**
     * Listens everytime the user receives a msg
     **/
    @Override
    public void messageReceived(Message message) {
        System.out.println("MSG -> " + message.plaintext);
        if(message.plaintext.contains("GroupMessage")){
            handleGroupMessages(message);
            return;
        }
        if (openedChat != null && openedChat.getReceiverUsername().equals(message.peerUsername)) {
            //send the msg to the open chat window to be displayed
            openedChat.showMessageReceived(message);
        } else {
            //changes the user tile to another color, because of unread message
            storeUnreadMessages(message,null);
            UnreadMessagesCellRenderer.unreadMessage(message.peerUsername);
            SwingUtilities.invokeLater(() -> {
                mainPanel.revalidate();
                mainPanel.repaint();
            });
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
            if(peerConnectionsValues.get(i).equals(peerConnection)) {
                user.peerConnections.remove(peerConnectionsUsers.get(i));
                tropa = peerConnectionsUsers.get(i);
            }
        }
    }

    private void handleGroupMessages(Message message) {
        if(message.plaintext.contains("GroupMessageInvitation")){
            String[] messageParts = message.plaintext.split("@");
            String groupName = messageParts[1];
            String cleanedMembersList = messageParts[2].replaceAll("[\\[\\]\"]", "");
            String[] membersArray = cleanedMembersList.split(",\\s*");
            ArrayList<String> members = new ArrayList<>(Arrays.asList(membersArray));
            checkInvitation(message.peerUsername,groupName,members);
        }else {
            String[] messageParts = message.plaintext.split("@");
            String groupName = messageParts[1];
            String messagePlaintext = messageParts[2];
            Message messageCleaned = new Message(messagePlaintext,message.sender,message.peerUsername);
            handleMessagesFromGroupChats(groupName,messageCleaned);
        }
    }

    private void handleMessagesFromGroupChats(String groupName,Message message) {
        if (openedGroup != null && openedGroup.getGroupName().equals(groupName)) {
            openedGroup.showMessageReceived(message);
        }else {
            storeUnreadMessages(message,groupName);
            UnreadMessagesCellRenderer.unreadMessage(groupName);
            SwingUtilities.invokeLater(() -> {
                mainPanel.revalidate();
                mainPanel.repaint();
            });
        }
    }

    private void checkInvitation(String host,String groupName,ArrayList<String> members){
        boolean hasHostInContacts = false;
        for (int i = 0; i < conversationsList.getModel().getSize(); i++) {
            String username = conversationsList.getModel().getElementAt(i);
            if(username.equals(host)) {
                hasHostInContacts = true;
                break;
            }
        }
        //Someone out of my contacts tried to add me in a new group
        if(!hasHostInContacts) {
            return;
        }
        addGroupChat(groupName,members);
    }

    private void storeUnreadMessages(Message message, String groupName) {
        String senderUsername = message.peerUsername;
        if(groupName != null) {
            senderUsername = groupName;
        }
        ArrayList<Message> messages;
        if (!unreadMessages.containsKey(senderUsername)) {
            messages = new ArrayList<>();
        } else {
            messages = unreadMessages.get(senderUsername);
        }
        messages.add(message);
        unreadMessages.put(senderUsername, messages);
    }


    public void deleteStoredUnreadMessages(String username) {
        unreadMessages.remove(username);
    }

    public HashMap<String, ArrayList<Message>> getUnreadMessages() {
        return unreadMessages;
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


