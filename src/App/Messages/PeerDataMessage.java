package App.Messages;

import Utils.InvalidMessageException;

public class PeerDataMessage extends Message {
    private String peerIp;
    private String peerPort;
    private String peerPublicKey;

    public PeerDataMessage(String peerIp, String peerPort, String peerPublicKey) {
        super(MessageType.PEER_DATA);
        this.peerIp = peerIp;
        this.peerPort = peerPort;
        this.peerPublicKey = peerPublicKey;
    }

    public String encode() {
        return type + "@" + peerIp + "@" + peerPort + "@" + peerPublicKey;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 4 && parts[0].equals(type.toString())) {
            this.peerIp = parts[1];
            this.peerPort = parts[2];
            this.peerPublicKey = parts[3];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static PeerDataMessage fromString(String message) throws InvalidMessageException {
        PeerDataMessage peerDataMessage = new PeerDataMessage("", "", "");
        peerDataMessage.decode(message);
        return peerDataMessage;
    }

    public String getPeerIp() {
        return peerIp;
    }

    public String getPeerPort() {
        return peerPort;
    }

    public String getPeerPublicKey() {
        return peerPublicKey;
    }
}
