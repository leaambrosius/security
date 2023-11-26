package App.Client;

import App.Storage.Message;
import App.Storage.MessagesRepository;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class ConnectionB {
    public static void main(String[] args) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, InvalidKeyException, ClassNotFoundException {
        String serverPort = "12345";
        String serverIP = "localhost";
        String localPort = "54322";
        Peer user = new Peer("B", serverIP, serverPort, localPort);
        user.announceToServer();
        MessagesRepository messageRepository = new MessagesRepository();

        ArrayList<String> nameList = new ArrayList<>();
        nameList.add("Alice");
        nameList.add("Bob");
        nameList.add("Charlie");
        nameList.add("David");

        messageRepository.addChatRooms(nameList);

        Message message = new Message("hi","name", "Alice");
        Message message1 = new Message("sdf","name", "Alice");
        Message message3 = new Message("he","name", "Alice");
        Message message2 = new Message("hello","name", "Bob");
        messageRepository.addMessage(message);
        messageRepository.addMessage(message1);
        messageRepository.addMessage(message2);
        messageRepository.addMessage(message3);

        List<Message> messageList = messageRepository.getChatHistory("Bob");

        for (Message out : messageList) {
            System.out.println(out.plaintext);
        }

        messageRepository.saveAndEncryptRepository();

        messageRepository.deleteChatrooms();

        messageRepository.decryptChatRooms("Alice");

        messageList = messageRepository.getChatHistory("Alice");

        for (Message out : messageList) {
            System.out.println("after dec:"+ out.plaintext);
        }

    }
}

