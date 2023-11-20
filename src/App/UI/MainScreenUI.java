package App.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

public class MainScreenUI {
    private JFrame frame;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private JList<String> conversationsList;
    private JButton startConversationButton;
    private List<ConversationViewUI> activeConversations;


    public MainScreenUI() {
        initialize();
        activeConversations = new ArrayList<>();
        setVisible(true);
    }

    private void initialize() {
        frame = new JFrame("Secure Chat");
        frame.setSize(400, 400);
        centerFrameOnScreen(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        JPanel mainPanel = new JPanel();

        mainPanel.setLayout(new BorderLayout());

        conversationsList = new JList<>(new String[]{"Add User", "Username 1", "Username 2", "Username 3", "Username 4"});
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
        ConversationViewUI conversationView = new ConversationViewUI(user);
        activeConversations.add(conversationView);
        conversationView.setVisible(true);
        frame.dispose();

    }

    private void showNewUserPanel() {
        JPanel newPanel = new JPanel();
        JTextField textField = new JTextField(20);
        JButton submitButton = new JButton("Submit");

        submitButton.addActionListener(e -> newUser(textField));

        newPanel.add(new JLabel("Enter User Name: "));
        newPanel.add(textField);
        newPanel.add(submitButton);

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


    public void newUser(JTextField textField){
        String inputText = textField.getText();
        checkNewUser(inputText);
        openConversationView(inputText);
    }
    private static void checkNewUser(String username) {
        // TODO check if user exits otherwise set error message
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }


    public static void main(String[] args) {
        MainScreenUI mainScreen = new MainScreenUI();

    }

}
