package App.Messages;

import Utils.InvalidMessageException;

public class HandshakeMessage extends Message {
    private String sessionSymmetricKey;
    private String storageSymmetricKey;

    public HandshakeMessage(String sessionSymmetricKey, String storageSymmetricKey) {
        super(MessageType.HANDSHAKE);
        this.sessionSymmetricKey = sessionSymmetricKey;
        this.storageSymmetricKey = storageSymmetricKey;
    }

    public String encode() {
        return type + "@" + sessionSymmetricKey + "@" + storageSymmetricKey;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 3 && parts[0].equals(type.toString())) {
            this.sessionSymmetricKey = parts[1];
            this.storageSymmetricKey = parts[2];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static HandshakeMessage fromString(String message) throws InvalidMessageException {
        HandshakeMessage handshakeMessage = new HandshakeMessage("", "");
        handshakeMessage.decode(message);
        return handshakeMessage;
    }

    public String getSessionSymmetricKey() {
        return sessionSymmetricKey;
    }

    public String getStorageSymmetricKey() {
        return storageSymmetricKey;
    }
}
