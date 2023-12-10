package App.Storage;

import App.Client.Peer;
import App.UI.MessageObserver;

import java.util.*;
import java.util.stream.Collectors;

public class MessagesRepository {
    // Chat ID -> Messages
    public HashMap<String, ArrayList<StorageMessage>> chatsHistory = new HashMap<>();
    public HashMap<String, ChatRecord> chats = new HashMap<>();
    public HashMap<String, ChatRecord> peerToChat = new HashMap<>();
    public HashMap<String, GroupRecord> groups = new HashMap<>();
    public HashMap<String, MessageObserver> listeners = new HashMap<>();

    // Chat ID -> last stored
    public HashMap<String, Set<String>> remoteStored = new HashMap<>();

    private static MessagesRepository messagesRepository; // Singleton instance

    public static MessagesRepository mr() {
        if (messagesRepository == null) {
            messagesRepository = new MessagesRepository();
        }
        return messagesRepository;
    }

    public MessagesRepository() {
        ArrayList<ChatRecord> chatRecords = FileManager.readContacts();
        for (ChatRecord r : chatRecords) {
            chats.put(r.chatId, r);
            peerToChat.put(r.peer, r);
            chatsHistory.put(r.chatId, new ArrayList<>());
        }

        ArrayList<GroupRecord> groupRecords = FileManager.readGroupContacts();
        for (GroupRecord r : groupRecords) {
            groups.put(r.chatId, r);
            chatsHistory.put(r.chatId, new ArrayList<>());
        }
    }

    public void addMessage(StorageMessage message) {
        if (chatsHistory.containsKey(message.chatId)) {
            chatsHistory.get(message.chatId).add(message);

            MessageObserver o = listeners.get(message.chatId);
            if (o != null) o.updateMessage(message);
        }
    }

    public void addMultipleMessages(String chatId, ArrayList<StorageMessage> mList) {
        MessagesRepository.mr().updateStored(chatId, mList);

        ArrayList<StorageMessage> history = chatsHistory.get(chatId);
        ArrayList<StorageMessage> mergedArray = new ArrayList<>();
        mergedArray.addAll(history);
        mergedArray.addAll(mList);

        // Remove duplicates
        Set<String> uniqueFieldValues = new HashSet<>();
        ArrayList<StorageMessage> uniqueList = (ArrayList<StorageMessage>) mergedArray.stream()
                .filter(obj -> uniqueFieldValues.add(obj.messageId))
                .collect(Collectors.toList());

        // Sort by timestamp
        uniqueList.sort(Comparator.comparing(StorageMessage::getTimestamp));
        chatsHistory.put(chatId, uniqueList);

        MessageObserver o = listeners.get(chatId);
        if (o != null) o.updateAll(uniqueList);
    }

    public void addGroup(GroupRecord group) {
        if (!groups.containsKey(group.chatId)) {
            groups.put(group.chatId, group);
            chatsHistory.put(group.chatId, new ArrayList<>());
            FileManager.saveGroup(group);
        }
    }

    public void addChat(ChatRecord chat, Peer host) {
        if (!chats.containsKey(chat.chatId)) {
            chats.put(chat.chatId, chat);
            peerToChat.put(chat.peer, chat);
            chatsHistory.put(chat.chatId, new ArrayList<>());
            FileManager.saveContact(chat);
            host.registerChatToRemote(chat.chatId);
        }
    }

    public ArrayList<StorageMessage> getChatHistory(String chatId) {
        return chatsHistory.getOrDefault(chatId, new ArrayList<>());
    }

    public ArrayList<StorageMessage> getChatHistoryForRemote(String chatId) {
        ArrayList<StorageMessage> history = chatsHistory.getOrDefault(chatId, new ArrayList<>());
        ArrayList<StorageMessage> toStore = new ArrayList<>();
        Set<String> storedIds = remoteStored.getOrDefault(chatId, null);

        if (storedIds == null) {
            storedIds = new HashSet<>();
            remoteStored.put(chatId, storedIds);
            return history;
        }
        for (StorageMessage m : history) {
            if (!storedIds.contains(m.messageId)) {
                toStore.add(m);
            }
        }
        return toStore;
    }
    public void updateStored(String chatId, ArrayList<StorageMessage> storedHistory) {
        Set<String> storedIds = remoteStored.getOrDefault(chatId, new HashSet<>());
        for (StorageMessage m : storedHistory) {
            storedIds.add(m.messageId);
        }
    }
    public String getStorageKey(String name) {
        if (peerToChat.containsKey(name)) {
            return peerToChat.get(name).symmetricKey;
        } else if (groups.containsKey(name)) {
            return groups.get(name).symmetricKey;
        }
        return "";
    }

    public String getStorageKeyByChat(String chatId) {
        if (chats.containsKey(chatId)) {
            return chats.get(chatId).symmetricKey;
        } else if (groups.containsKey(chatId)) {
            return groups.get(chatId).symmetricKey;
        }
        return "";
    }

    public String getChatId(String name) {
        if (peerToChat.containsKey(name)) {
            return peerToChat.get(name).chatId;
        }
        return null;
    }

    public void subscribe(MessageObserver o, String chatId) {
        this.listeners.put(chatId, o);
    }
}
