package App.Client;

import java.security.PublicKey;

public class PeerData {
    public final String username;
    public final String address;
    public final String port;
    public final PublicKey publicKey;

    public PeerData(String username, String address, String port, PublicKey publicKey) {
        this.username = username;
        this.address = address;
        this.port = port;
        this.publicKey = publicKey;
    }
}
