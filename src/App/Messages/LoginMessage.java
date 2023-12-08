package App.Messages;

import Utils.InvalidMessageException;

public class LoginMessage extends Message {
    private String username;
    private String localPort;
    private String signature;

    public LoginMessage(String username, String localPort, String signature) {
        super(MessageType.LOGIN);
        this.username = username;
        this.localPort = localPort;
        this.signature = signature;
    }

    public String encode() {
        return type + "@" + username + "@" + localPort + "@" + signature;
    }

    public void decode(String message) throws InvalidMessageException {
        String[] parts = message.split("@");
        if (parts.length == 4 && parts[0].equals(type.toString())) {
            this.username = parts[1];
            this.localPort = parts[2];
            this.signature = parts[3];
        } else {
            throw new InvalidMessageException("Bad message format: " + message);
        }
    }

    public static LoginMessage fromString(String message) throws InvalidMessageException {
        LoginMessage loginMessage = new LoginMessage("", "", "");
        loginMessage.decode(message);
        return loginMessage;
    }

    public String getSignature() {
        return signature;
    }

    public String getUsername() {
        return username;
    }

    public String getLocalPort() {
        return localPort;
    }

    public static String getNACK() {
        return MessageType.LOGIN + "@NACK";
    }
}
