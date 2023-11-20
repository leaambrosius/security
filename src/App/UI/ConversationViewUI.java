package App.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


public class ConversationViewUI {
    private JFrame frame;
    private JTextArea messageDisplayArea;
    private JTextField messageInputField;
    private JButton sendButton;
    private JButton backButton;

    public ConversationViewUI(String recipientName) {
        initialize(recipientName);
    }

    private void initialize(String recipientName) {
        frame = new JFrame("Conversation with " + recipientName);

        frame.setSize(400, 400);
        MainScreenUI.centerFrameOnScreen(frame);

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
        keyListener();

        frame.getContentPane().add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(backButton, BorderLayout.NORTH);
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


    public void actionlistener(){

        sendButton.addActionListener(e -> sendMessage());

        backButton.addActionListener(e -> {
            frame.dispose();
            new MainScreenUI();
        });

    }

    public void appendMessage(String sender, String message) {
        messageDisplayArea.append(sender + ": " + message + "\n");
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public void sendMessage(){
        String message = messageInputField.getText();
        if (!message.isEmpty()) {
            messageDisplayArea.append("Me: " + message + "\n");
            messageInputField.setText("");
        }
    }

    public static void main(String[] args) {
        ConversationViewUI conversationView = new ConversationViewUI("User 2");
        conversationView.setVisible(true);
        // for testing to receive message
        conversationView.appendMessage("Name", "hello");

    }
}