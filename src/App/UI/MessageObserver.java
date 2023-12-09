package App.UI;

import App.Storage.StorageMessage;

import java.util.ArrayList;

public interface MessageObserver {

    void updateMessage(StorageMessage m);

    void updateAll(ArrayList<StorageMessage> m);
}
