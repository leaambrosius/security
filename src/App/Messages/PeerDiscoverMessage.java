package App.Messages;

import Utils.InvalidMessageException;

public class PeerDiscoverMessage extends Message {
    private String peerUsername;

    public PeerDiscoverMessage(String peerUsername) {
        super(MessageType.PEER_DISCOVER);
        this.peerUsername = peerUsername;
    }

    public String encode() {
        return type + "@" + peerUsername;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 2 && parts[0].equals(type.toString())) {
            this.peerUsername = parts[1];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static PeerDiscoverMessage fromString(String message) throws InvalidMessageException {
        PeerDiscoverMessage peerDiscoverMessage = new PeerDiscoverMessage("");
        peerDiscoverMessage.decode(message);
        return peerDiscoverMessage;
    }

    public String getPeerUsername() {
        return peerUsername;
    }

    public static String getNACK() {
        return MessageType.PEER_DISCOVER + "@NACK";
    }
}
