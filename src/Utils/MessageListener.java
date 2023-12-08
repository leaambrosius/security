package Utils;

import App.Client.PeerConnection;
import App.Messages.Message;

public interface MessageListener {
    void messageReceived(Message message, String peerUsername);

    void connectionEnded(PeerConnection peerConnection);

    void userOffline(String user);
}
