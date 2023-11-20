package App.Client;

import App.Storage.Message;
import App.Storage.MessagesRepository;

public class ConnectionB {
    public static void main(String[] args) {
        String serverPort = "12345";
        String serverIP = "localhost";
        String localPort = "54322";
        Peer user = new Peer("B", serverIP, serverPort, localPort);
        user.announceToServer();
        MessagesRepository messageRepository = new MessagesRepository();
        Message message = new Message("hi","A", "name");
        messageRepository.addMessage(message);
        messageRepository.getChatHistory("name");
//        user.connectToPeer("ola");
    }
}

