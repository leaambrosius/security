package App.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class RemoteStorage {
    private static ArrayList<String> getChatIdsForUsername(String username) throws SQLException {
        Connection connection = CloudConnectionPoolFactory.ds().getConnection();
        ArrayList<String> chatIds = new ArrayList<>();
        String query = "SELECT chat_id FROM chats WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                chatIds.add(resultSet.getString("chat_id"));
            }
        }
        return chatIds;
    }

    private static ArrayList<String> getMessagesForChatId(String chatId) throws SQLException {
        Connection connection = CloudConnectionPoolFactory.ds().getConnection();

        ArrayList<String> messages = new ArrayList<>();
        String query = "SELECT message FROM messages WHERE chat_id = ? ORDER BY timestamp";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                messages.add(resultSet.getString("message"));
            }
        }
        return messages;
    }

    private static ArrayList<String> getMessagesForKeywordAndChatId(String keyword, String chatId) throws SQLException {
        Connection connection = CloudConnectionPoolFactory.ds().getConnection();

        ArrayList<String> messages = new ArrayList<>();
        String query = "SELECT message FROM messages JOIN keywords ON messages.message_id = keywords.message_id " +
                "WHERE keywords.keyword = ? AND messages.chat_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, keyword);
            statement.setString(2, chatId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                messages.add(resultSet.getString("message"));
            }
        }
        return messages;
    }

    public static void insertChat(String chatId, String username) throws SQLException {
        Connection connection = CloudConnectionPoolFactory.ds().getConnection();

        String query = "INSERT INTO chats (chat_id, username) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, chatId);
            statement.setString(2, username);
            statement.executeUpdate();
        }
    }

    public static void insertKeywordMessage(String keyword, String messageId) throws SQLException {
        Connection connection = CloudConnectionPoolFactory.ds().getConnection();

        String query = "INSERT INTO keywords (keyword, message_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, keyword);
            statement.setString(2, messageId);
            statement.executeUpdate();
        }
    }

    public static void insertMessages(String messageId, String timestamp, String sender, String chatId, String message, String signature) throws SQLException {
        Connection connection = CloudConnectionPoolFactory.ds().getConnection();

        String query = "INSERT INTO messages (message_id, timestamp, sender, chat_id, message, signature) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, messageId);
            statement.setString(2, timestamp);
            statement.setString(3, sender);
            statement.setString(4, chatId);
            statement.setString(5, message);
            statement.setString(6, signature);
            statement.executeUpdate();
        }
    }
}

