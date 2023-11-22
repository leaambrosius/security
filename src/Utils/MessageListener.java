package Utils;

import App.Storage.Message;

public interface MessageListener {
    void messageReceived(Message message);
}
