package App.Messages;

import Utils.InvalidMessageException;

// GROUP_INVITATION @ GROUP_NAME @ MEMBERS
public class GroupInvitationMessage extends Message {
    private String groupName;
    private String members;

    public GroupInvitationMessage(String groupName, String members) {
        super(MessageType.GROUP_INVITATION);
        this.groupName = groupName;
        this.members = members;
    }

    public String encode() {
        return type + "@" + groupName + "@" + members;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 3 && parts[0].equals(type.toString())) {
            this.groupName = parts[1];
            this.members = parts[2];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static GroupInvitationMessage fromString(String message) throws InvalidMessageException {
        GroupInvitationMessage groupInvitationMessage = new GroupInvitationMessage("", "");
        groupInvitationMessage.decode(message);
        return groupInvitationMessage;
    }

    public String getMembers() {
        return members;
    }

    public String getGroupName() {
        return groupName;
    }

}

