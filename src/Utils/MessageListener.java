package Utils;

import App.Client.PeerConnection;
import App.Storage.Message;

public interface MessageListener {
    void messageReceived(Message message);

    void connectionEnded(PeerConnection peerConnection);
}
