package App.Messages;

import Utils.InvalidMessageException;

public class RegisterMessage extends Message {
    private String username;
    private String localPort;
    private String publicKey;

    public RegisterMessage(String username, String localPort, String publicKey) {
        super(MessageType.REGISTER);
        this.username = username;
        this.localPort = localPort;
        this.publicKey = publicKey;
    }

    public String encode() {
        return type + "@" + username + "@" + localPort + "@" + publicKey;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 4 && parts[0].equals(type.toString())) {
            this.username = parts[1];
            this.localPort = parts[2];
            this.publicKey = parts[3];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static RegisterMessage fromString(String message) throws InvalidMessageException {
        RegisterMessage registerMessage = new RegisterMessage("", "", "");
        registerMessage.decode(message);
        return registerMessage;
    }

    public String getUsername() {
        return username;
    }

    public String getLocalPort() {
        return localPort;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public static String getNACK() {
        return MessageType.REGISTER + "@NACK";
    }
}
