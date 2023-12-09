package App.Storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RemoteStorage {
    static Logger logger = Logger.getLogger(RemoteStorage.class.getName());

    public static boolean hasUserAccessToChat(String username, String chatId) throws SQLException {
        String query = "SELECT COUNT(*) AS user_exists FROM chats WHERE chat_id = ? AND username = ?";
        Connection connection = CloudConnectionPoolFactory.getConnection();

        // Create PreparedStatement
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, chatId);
            statement.setString(2, username);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int userExists = resultSet.getInt("user_exists");
                return userExists == 1;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to execute hasUserAccessToChat query " + e);
        } finally {
            connection.close();
        }
        return false;
    }

    public static ArrayList<String> getChatMessages(String chatId) throws SQLException {
        ArrayList<String> messages = new ArrayList<>();
        String query = "SELECT * FROM messages WHERE chat_id = ? ORDER BY timestamp";
        Connection connection = CloudConnectionPoolFactory.getConnection();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String messageId = resultSet.getString("message_id");
                String timestamp = resultSet.getString("timestamp");
                String encryptedSender = resultSet.getString("sender");
                String encryptedMessage = resultSet.getString("message");
                String signature = resultSet.getString("signature");
                StorageMessage message = new StorageMessage(messageId, timestamp, encryptedSender, chatId, encryptedMessage, signature);
                messages.add(message.serialize());
            }
        } catch (SQLException | IOException e) {
            logger.log(Level.WARNING, "Failed to execute getChatMessages query");
        } finally {
            connection.close();
        }
        return messages;
    }

    public static void insertChat(String chatId, String username) throws SQLException {
        Connection connection = CloudConnectionPoolFactory.getConnection();
        String query = "INSERT INTO chats (chat_id, username) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, chatId);
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to insert chat to remote storage");
        } finally {
            connection.close();
        }
    }

    public static void insertMessages(ArrayList<StorageMessage> messages) throws SQLException {
        Connection connection = CloudConnectionPoolFactory.getConnection();

        String query = "INSERT INTO messages (message_id, timestamp, sender, chat_id, message, signature) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (message_id) DO NOTHING";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (StorageMessage msg : messages) {
                statement.setString(1, msg.messageId);
                statement.setString(2, msg.timestamp);
                statement.setString(3, msg.sender);
                statement.setString(4, msg.chatId);
                statement.setString(5, msg.message);
                statement.setString(6, msg.signature);
                statement.addBatch();
            }
            statement.executeBatch();
        } finally {
            connection.close();
        }
    }

    public static ArrayList<String> getMessagesForKeywordAndChatId(String keyword, String chatId) throws SQLException {
        Connection connection = CloudConnectionPoolFactory.getConnection();

        ArrayList<String> messages = new ArrayList<>();
        String query = "SELECT * FROM messages JOIN keywords ON messages.message_id = keywords.message_id " +
                "WHERE keywords.keyword = ? AND messages.chat_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, keyword);
            statement.setString(2, chatId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String messageId = resultSet.getString("message_id");
                String timestamp = resultSet.getString("timestamp");
                String encryptedSender = resultSet.getString("sender");
                String encryptedMessage = resultSet.getString("message");
                String signature = resultSet.getString("signature");
                StorageMessage message = new StorageMessage(messageId, timestamp, encryptedSender, chatId, encryptedMessage, signature);
                messages.add(message.serialize());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
        return messages;
    }

    public static void insertKeywordMessage(String keyword, String messageId) throws SQLException {
        Connection connection = CloudConnectionPoolFactory.getConnection();

        String query = "INSERT INTO keywords (keyword, message_id) VALUES (?, ?) ON CONFLICT (keyword, message_id) DO NOTHING";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, keyword);
            statement.setString(2, messageId);
            statement.executeUpdate();
        } finally {
            connection.close();
        }
    }
}

