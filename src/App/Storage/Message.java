package App.Storage;

public class Message {
    public final String plaintext;
    public final String peerUsername;
    public final String sender;
    public final long timestamp;

    public Message(String plaintext, String sender, String peerUsername) {
        this.plaintext = plaintext;
        this.peerUsername = peerUsername;
        this.sender = sender;
        this.timestamp = System.currentTimeMillis();
    }
}
