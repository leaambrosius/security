package App.Messages;

import Utils.InvalidMessageException;

public class PeerAnnouncementMessage extends Message {
    private String username;

    public PeerAnnouncementMessage(String username) {
        super(MessageType.PEER_ANNOUNCEMENT);
        this.username = username;
    }

    public String encode() {
        return type + "@" + username;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 2 && parts[0].equals(type.toString())) {
            this.username = parts[1];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static PeerAnnouncementMessage fromString(String message) throws InvalidMessageException {
        PeerAnnouncementMessage peerAnnouncementMessage = new PeerAnnouncementMessage("");
        peerAnnouncementMessage.decode(message);
        return peerAnnouncementMessage;
    }

    public String getUsername() {
        return username;
    }
}