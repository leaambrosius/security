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
        if (chatRooms.containsKey(peer)) {
            return chatRooms.get(peer);
        } else {
            return new ArrayList<>();
        }
    }


    public void saveAndEncryptRepository() {
        // TODO
    }

}
