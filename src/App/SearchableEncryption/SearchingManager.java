package App.SearchableEncryption;

import App.Storage.MessagesRepository;
import App.Storage.StorageMessage;
import App.UI.MessageObserver;
import opennlp.tools.stemmer.PorterStemmer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class SearchingManager {

    // keyword -> opened chat
    public static HashMap<String, MessageObserver> listeners = new HashMap<>();

    private static final PorterStemmer porterStemmer = new PorterStemmer();

   public static String getKeyword(String query){
       return porterStemmer.stem(query);
   }

   public static void putMessages(ArrayList<String> messagesIds, String keyword, String chatId){
      ArrayList<StorageMessage> history = MessagesRepository.mr().getChatHistory(chatId);
      ArrayList<StorageMessage> filteredHistory = (ArrayList<StorageMessage>)
              history.stream().filter(m-> messagesIds.contains(m.messageId)).toList();
      for(StorageMessage m : filteredHistory){
         if(m.message.contains(keyword) && listeners.containsKey(keyword)){
             listeners.get(keyword).updateMessage(m);
         }
      }
   }

   public static void subscribe(String keyword,MessageObserver o){
       listeners.put(keyword,o);
   }

}
