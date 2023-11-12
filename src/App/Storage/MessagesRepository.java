package App.Storage;

import java.util.ArrayList;
import java.util.HashMap;

// TODO
public class MessagesRepository {
    HashMap<String, ArrayList<Message>> chatRooms = new HashMap<>();

    public void addMessage(Message message) {
        String peer = message.peerUsername;
        if (chatRooms.containsKey(peer)) {
            ArrayList<Message> chatRoomHistory = chatRooms.get(peer);
            chatRoomHistory.add(message);
        }
    }

    public ArrayList<Message> getChatHistory(String peer) {
        // TODO
        return null;
    }

    public void saveAndEncryptRepository() {
        // TODO
    }

}
