package App.UI;
import App.Client.Peer;
import App.Storage.MessagesRepository;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SearchChatUI extends JFrame {

    private JTextField searchField;
    private JTextArea resultList;

    public SearchChatUI(String recipientName, Peer user) {
        setTitle("Search Chat UI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        JButton backButton = new JButton("Back");
        resultList = new JTextArea(10, 40);

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch();
            }
        });

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(searchField);
        panel.add(searchButton);
        panel.add(backButton);
        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(resultList), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void performSearch() {
        String query = searchField.getText();
        resultList.setText("Search Results for: " + query);
    }



}
