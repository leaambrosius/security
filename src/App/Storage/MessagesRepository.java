package App.Storage;

import java.util.ArrayList;
import java.util.HashMap;

// TODO
public class MessagesRepository {
    HashMap<String, ArrayList<Message>> chatRooms = new HashMap<>();

    public void addMessage(Message message) {
        String peer = message.peerUsername;

        System.out.println("hist1:"+chatRooms);
        if (chatRooms.containsKey(peer)) {
            ArrayList<Message> chatRoomHistory = chatRooms.get(peer);
            chatRoomHistory.add(message);
            System.out.println("hist:"+chatRoomHistory);
        }
    }

    public ArrayList<Message> getChatHistory(String peer) {
        if (chatRooms.containsKey(peer)) {
            System.out.println(chatRooms.get(peer));
            return chatRooms.get(peer);
        } else {
            return new ArrayList<>();
        }
    }


    public void saveAndEncryptRepository() {
        // TODO should the keyrepo be used therefore?
        /* all messages stored in one file? what should it look like

         */
    }

}
