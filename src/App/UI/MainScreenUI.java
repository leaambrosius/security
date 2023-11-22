package App.UI;

import App.Client.Peer;
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
import java.util.ArrayList;
import java.util.HashMap;
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
        frame = new JFrame("Secure Chat");
        frame.setSize(400, 400);
        centerFrameOnScreen(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        mainPanel = new JPanel();

        mainPanel.setLayout(new BorderLayout());

        //TODO delete test users
        conversationsList = new JList<>(new String[]{"Add User", "Username 1", "Username 2", "Username 3", "Username 4"});
        conversationsList.setCellRenderer(new UnreadMessagesCellRenderer());
        if (currentModel != null) {
            conversationsList.setModel(currentModel);
        }
        mainPanel.add(new JScrollPane(conversationsList), BorderLayout.CENTER);

        startConversationButton = new JButton("Start Conversation");

        actionlistener();

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

    public void actionlistener() {
        startConversationButton.addActionListener(e -> {
            String selectedRecipient = conversationsList.getSelectedValue();
            if (selectedRecipient == null || selectedRecipient.equals("Add User")) {
                showNewUserPanel();
            } else {
                openConversationView(selectedRecipient);
            }
        });
    }

    private void openConversationView(String user) {
        ConversationViewUI conversationView = new ConversationViewUI(this, user, currentModel, this.user);
        activeConversations.add(conversationView);
        conversationView.setVisible(true);
        openedChat = conversationView;
        UnreadMessagesCellRenderer.readMessage(user);
        //frame.dispose();
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
            return; //TODO give error trying to find user in server
        }
        cardPanel.remove(cardPanel.getComponentCount() - 1);
        addNewUser(inputText);
        //openConversationView(inputText);
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

    private void addNewUser(String username) {
        if (currentModel != null) {
            currentModel.addElement(username);
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

        newModel.addElement(username);
        currentModel = newModel;

        //If we dont need to open the conversation UI
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
        if (openedChat != null && openedChat.getReceiverUsername().equalsIgnoreCase(message.peerUsername)) {
            //send the msg to the open chat window to be displayed
            openedChat.showMessageReceived(message);
        } else {
            //changes the user tile to another color, because of unread message
            storeUnreadMessages(message);
            UnreadMessagesCellRenderer.unreadMessage(message.peerUsername);
            SwingUtilities.invokeLater(() -> {
                mainPanel.revalidate();
                mainPanel.repaint();
            });
        }
    }

    private void storeUnreadMessages(Message message){
        String senderUsername = message.peerUsername;
        ArrayList<Message> messages;
        if(!unreadMessages.containsKey(senderUsername)){
            messages = new ArrayList<>();
        }else {
            messages = unreadMessages.get(senderUsername);
        }
        messages.add(message);
        unreadMessages.put(senderUsername,messages);
    }

    public void deleteStoredUnreadMessages(String username){
        unreadMessages.remove(username);
    }

    public HashMap<String,ArrayList<Message>> getUnreadMessages(){
        return unreadMessages;
    }

    public void placeFrameInCoordinates(int x, int y) {
        frame.setLocation(x, y);
    }

    private static class UnreadMessagesCellRenderer extends DefaultListCellRenderer {

        private static HashMap<String, Boolean> unreadMessages = new HashMap<>();

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (unreadMessages.containsKey(value.toString())){
                Boolean unread = unreadMessages.get(value.toString());
                if(unread){
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


