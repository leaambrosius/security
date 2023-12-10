package App.UI;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

public class SelectGroupChatMembers extends JFrame {
    private JTextField groupName;
    private JTextField members;
    private JButton submitButton;

    ArrayList<String> users = new ArrayList<>();
    private JComboBox<String> dropdown;

    private JList<String> itemList;

    private DefaultListModel<String> listModel;
    private MainScreenUI mainScreenUI;


    public SelectGroupChatMembers(MainScreenUI mainScreenUI, ArrayList<String> usersList) {
        this.mainScreenUI = mainScreenUI;
        getUsers(usersList);
        initialize();
    }

    private void getUsers(ArrayList<String> conversationsList) {
        //TODO ERROR -> group chat window -> submit with null input
        for (int i = 0; i < conversationsList.size(); i++) {
            String username = conversationsList.get(i);
            if(!username.equals("Add User") && ! username.equals("Create group chat")) {
                users.add(username);
            }
        }
        listModel = new DefaultListModel<>();
        for (String user : users) {
            listModel.addElement(user);
        }
        //dropdown = new JComboBox<String>(users.toArray(new String[0]));
    }

    private void initialize() {
        setTitle("Input Window");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        groupName = new JTextField(20);
        members = new JTextField(20);
        submitButton = new JButton("Submit");

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userInput1 = groupName.getText();
                java.util.List<String> selectedOptions = itemList.getSelectedValuesList();
                if(userInput1 != null /*&& userInput2 != null*/ && !userInput1.isEmpty()/*&& !userInput2.isEmpty()*/) {
                    mainScreenUI.createGroupChat(userInput1, (ArrayList<String>) selectedOptions);
                    dispose();
                }
            }
        });

        itemList = new JList<>(listModel);
        itemList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        JLabel adviceLabel = new JLabel("Use left click + CTRL to select multiple users");
        JLabel label1 = new JLabel("Group name:");
        JLabel label2 = new JLabel("Add members:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(adviceLabel, gbc);

        gbc.gridy = 1;
        panel.add(label1, gbc);

        gbc.gridy = 2;
        panel.add(label2, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(groupName, gbc);

        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(new JScrollPane(itemList), gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(submitButton, gbc);

        // Add the panel to the JFrame
        add(panel);

        // Set JFrame properties
        pack();
        setLocationRelativeTo(null); // Center the window
        setVisible(true);

        //TODO working but ugly
        /*JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1));
        JLabel label1 = new JLabel("Group name:");
        JLabel label2 = new JLabel("Add members:");
        panel.add(label1);
        panel.add(groupName);
        panel.add(label2);
        panel.add(new JScrollPane(itemList));
        panel.add(submitButton);
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);*/
    }
}