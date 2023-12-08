package App.Storage;

import org.checkerframework.checker.units.qual.A;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class FileManager {
    static final String CONTACTS_FILENAME = "contacts.txt";
    static final String GROUPS_FILENAME = "groups.txt";

    public static ArrayList<ChatRecord> readContacts() {
        ArrayList<ChatRecord> chats = new ArrayList<>();

        if (!Files.exists(Path.of(CONTACTS_FILENAME))) return chats;

        try (BufferedReader br = new BufferedReader(new FileReader(CONTACTS_FILENAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("@");
                if (parts.length == 3) {
                    String chatId = parts[0];
                    String peer = parts[1];
                    String symmetricKey = parts[2];
                    chats.add(new ChatRecord(chatId, peer, symmetricKey));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chats;
    }

    public static ArrayList<GroupRecord> readGroupContacts() {
        ArrayList<GroupRecord> chats = new ArrayList<>();
        ArrayList<String> members = new ArrayList<>();

        if (!Files.exists(Path.of(GROUPS_FILENAME))) return chats;

        try (BufferedReader br = new BufferedReader(new FileReader(GROUPS_FILENAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("@");
                if (parts.length > 2) {
                    String groupName = parts[0];
                    String symmetricKey = parts[1];

                    members.clear();
                    members.addAll(Arrays.asList(parts).subList(2, parts.length));
                    chats.add(new GroupRecord(members, groupName, symmetricKey));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chats;
    }

    public static void saveContact(ChatRecord chat) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONTACTS_FILENAME, true))) {
            File file = new File(CONTACTS_FILENAME);
            if (file.exists() && file.length() > 0) {
                writer.newLine();
            }
            writer.write(chat.toStorageString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveGroup(GroupRecord group) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(GROUPS_FILENAME, true))) {
            File file = new File(GROUPS_FILENAME);
            if (file.exists() && file.length() > 0) {
                writer.newLine();
            }
            writer.write(group.toStorageString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
