package App.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConversationViewUI {
    private JFrame frame;
    private JTextArea messageDisplayArea;
    private JTextField messageInputField;
    private JButton sendButton;
    private JButton backButton;
    private String recipientName;

    public ConversationViewUI(String recipientName) {
        this.recipientName = recipientName;
        initialize(recipientName);
    }

    private void initialize(String recipientName) {
        frame = new JFrame("Conversation with " + recipientName);
        frame.setBounds(100, 100, 400, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        messageDisplayArea = new JTextArea();
        messageDisplayArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(messageDisplayArea), BorderLayout.CENTER);

        messageInputField = new JTextField();
        frame.getContentPane().add(messageInputField, BorderLayout.SOUTH);

        sendButton = new JButton("Send");

        backButton = new JButton("Back");

        actionlistener();

        frame.getContentPane().add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(backButton, BorderLayout.NORTH);
    }


    public void actionlistener(){

        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = messageInputField.getText();
                if (!message.equals("")) {
                    messageDisplayArea.append("Me: " + message + "\n");
                    messageInputField.setText("");
                }
            }
        });

        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                new MainScreenUI();
            }
        });

    }

    public void appendMessage(String sender, String message) {
        messageDisplayArea.append(sender + ": " + message + "\n");
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public static void main(String[] args) {
        ConversationViewUI conversationView = new ConversationViewUI("User 2");
        conversationView.setVisible(true);
        // for testing to receive message
        conversationView.appendMessage("Name", "hello");

    }
}