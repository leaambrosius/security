package App.UI;

import App.Storage.StorageMessage;

public interface MessageObserver {

    void updateMessage(StorageMessage m);
}
