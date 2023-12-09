package App.UI;
import App.Client.Peer;
import App.Messages.ChatMessage;
import App.Messages.SearchChatMessage;
import App.Storage.Message;
import App.Storage.MessagesRepository;
import App.Storage.StorageMessage;
import opennlp.tools.stemmer.PorterStemmer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SearchChatUI {
    private final MainScreenUI mainUI;
    private JFrame frame;
    private JTextField searchField;
    private JTextArea resultList;
    String recipientName;
    Peer user;
    JFrame convFrame;

    public SearchChatUI(JFrame convFrame,MainScreenUI mainUI, String recipientName, Peer user) {
        this.convFrame = convFrame;
        this.mainUI = mainUI;
        this.recipientName = recipientName;
        this.user = user;
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
            ConversationViewUI newFrame = new ConversationViewUI(mainUI,recipientName,user,x,y);
            newFrame.setVisible(true);

        });

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(searchField);
        panel.add(searchButton);
        panel.add(backButton);
        frame.add(panel, BorderLayout.NORTH);
        frame.add(new JScrollPane(resultList), BorderLayout.CENTER);

        frame.setVisible(true);

        System.out.println("search " +frame.getSize());
    }

    private void performSearch() {
        String query = searchField.getText();

        PorterStemmer porterStemmer = new PorterStemmer();
        String stem = porterStemmer.stem(query);

        String chatId = MessagesRepository.mr().getChatId(recipientName);

        // TODO stem needs to be encrypted, I'm not sure how
        SearchChatMessage message = new SearchChatMessage(recipientName,chatId,stem);
        //TODO search for the words entered and display the entrie message

        resultList.setText("Search Results for: " + stem);
    }

}
