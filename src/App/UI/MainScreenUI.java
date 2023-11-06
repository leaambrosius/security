package App.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class MainScreenUI {
    private JFrame frame;
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
        frame.setBounds(100, 100, 400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        conversationsList = new JList<>(new String[]{"User 1", "User 2", "Group 1", "Group 2"});
        frame.getContentPane().add(new JScrollPane(conversationsList), BorderLayout.CENTER);

        startConversationButton = new JButton("Start New Conversation");

        actionlistener();

        frame.getContentPane().add(startConversationButton, BorderLayout.SOUTH);
    }

    public void actionlistener(){
        startConversationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedRecipient = conversationsList.getSelectedValue();
                if (selectedRecipient != null) {
                    ConversationViewUI conversationView = new ConversationViewUI(selectedRecipient);
                    activeConversations.add(conversationView);
                    conversationView.setVisible(true);
                    frame.dispose();;
                }
            }
        });
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public static void main(String[] args) {
        MainScreenUI mainScreen = new MainScreenUI();

    }

}
