package App.UI;
import App.Client.Peer;
import App.SearchableEncryption.SearchingManager;
import App.Storage.MessagesRepository;
import App.Storage.StorageMessage;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class SearchChatUI implements MessageObserver {
    private final MainScreenUI mainUI;
    private JFrame frame;
    private JTextField searchField;
    private JTextArea resultList;
    String recipientName;
    Peer user;
    JFrame convFrame;
    String userOrGroup;

    ArrayList<String> recipientNames;

    public SearchChatUI(JFrame convFrame, MainScreenUI mainUI, String recipientName, Peer user, String userOrGroup, @Nullable ArrayList<String> recipientNames ) {
        this.convFrame = convFrame;
        this.mainUI = mainUI;
        this.recipientName = recipientName;
        this.user = user;
        this.userOrGroup = userOrGroup;
        this.recipientNames = recipientNames;
        init();
    }

    private void init() {
        frame = new JFrame("Search in Chat with: " + recipientName);
        frame.setSize(400, 400);


        //MainScreenUI.centerFrameOnScreen(frame);
        frame.setLocation(convFrame.getX(), convFrame.getY());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        searchField = new JTextField(15);
        JButton searchButton = new JButton("Search");
        JButton backButton = new JButton("Back");
        resultList = new JTextArea(10, 30);

        searchButton.addActionListener(e -> performSearch());

        backButton.addActionListener(e -> {
            int x = frame.getX();
            int y = frame.getY();
            frame.dispose();
if (userOrGroup.equals("user")) {
    ConversationViewUI newFrame = new ConversationViewUI(mainUI, recipientName, user, x, y);
    newFrame.setVisible(true);
} else if (userOrGroup.equals("group")){
    GroupChatViewUI newFrame = new GroupChatViewUI(mainUI,recipientNames,user,recipientName,x,y);
    newFrame.setVisible(true);
}



        });

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(searchField);
        panel.add(searchButton);
        panel.add(backButton);
        frame.add(panel, BorderLayout.NORTH);
        frame.add(new JScrollPane(resultList), BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void performSearch() {
        String query = searchField.getText();
        String chatId = MessagesRepository.mr().getChatId(recipientName);
        if (chatId == null){
            chatId = recipientName;
        }

        String stem = SearchingManager.getKeyword(query);

        SearchingManager.subscribe(query, this);
        resultList.setText("Search Results for: " + query + "\n");
        user.searchForKeyword(chatId, stem, query);
    }

    @Override
    public void updateMessage(StorageMessage m) {
        resultList.append(m.sender + ": " + m.message + "\n");
    }

    @Override
    public void updateAll(ArrayList<StorageMessage> mList) {
        for (StorageMessage m : mList) resultList.append(m.sender + ": " + m.message + "\n");
    }
}
