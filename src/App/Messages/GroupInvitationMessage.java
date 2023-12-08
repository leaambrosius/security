package App.Messages;

import Utils.InvalidMessageException;

import java.util.ArrayList;
import java.util.Arrays;

// GROUP_INVITATION @ GROUP_NAME @ STORAGE_KEY @ MEMBERS
public class GroupInvitationMessage extends Message {
    private String groupName;
    private String storageKey;
    private ArrayList<String> members;

    public GroupInvitationMessage(String groupName, ArrayList<String> members, String storageKey) {
        super(MessageType.GROUP_INVITATION);
        this.groupName = groupName;
        this.members = members;
        this.storageKey = storageKey;
    }

    public String encode() {
        return type + "@" + groupName + "@" + storageKey + "@" + String.join("@", members);
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length > 3 && parts[0].equals(type.toString())) {
            this.groupName = parts[1];
            this.storageKey = parts[2];
            members = new ArrayList<>();
            members.addAll(Arrays.asList(parts).subList(3, parts.length));
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static GroupInvitationMessage fromString(String message) throws InvalidMessageException {
        GroupInvitationMessage groupInvitationMessage = new GroupInvitationMessage("", null, "");
        groupInvitationMessage.decode(message);
        return groupInvitationMessage;
    }

    public ArrayList<String> getMembers() {
        return members;
    }

    public String getGroupName() {
        return groupName;
    }
    public String getStorageKey() {
        return storageKey;
    }

}

