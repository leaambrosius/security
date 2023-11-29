package App.UI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class UsernameSubmitUI extends JFrame {
    private JTextField textField;

    public UsernameSubmitUI() {
        initialize();
    }

    private void initialize() {
        setTitle("User name input");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(300, 150);

        textField = new JTextField(20);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userInput = textField.getText();
                if (userInput!= null && !userInput.isEmpty()) {
                    dispose();
                    App.App.runMainUI(userInput);
                }
            }
        });

        JPanel panel = new JPanel();
        panel.add(new JLabel("Enter your user name:"));
        panel.add(textField);
        panel.add(submitButton);
        add(panel);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}