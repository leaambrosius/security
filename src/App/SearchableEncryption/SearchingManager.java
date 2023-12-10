package App.SearchableEncryption;

import App.Storage.MessagesRepository;
import App.Storage.StorageMessage;
import App.UI.MessageObserver;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchingManager {
    static Logger logger = Logger.getLogger(SearchingManager.class.getName());


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

   public static ArrayList<String> getKeywords(String m) {
       ArrayList<String> extractedKeywords = new ArrayList<>();

       try {
           // Load English POS model
           POSModel posModel = new POSModel(Paths.get("resources/en-pos-maxent.bin"));
           POSTaggerME tagger = new POSTaggerME(posModel);

           // Tokenize the sentence
           Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
           String[] tokens = tokenizer.tokenize(m);

           // Tag parts of speech
           String[] tags = tagger.tag(tokens);

           // Define stop tags
           String[] stopTags = { "DT", "IN" }; // DT: Determiner, IN: Preposition

           // Remove stop words based on their POS tags
           for (int i = 0; i < tokens.length; i++) {
               if (!isStopTag(tags[i], stopTags)) {
                   extractedKeywords.add(tokens[i]);
               }
           }
       } catch (Exception e) {
           logger.log(Level.WARNING, "Could not tokenize the message");
       }
       return extractedKeywords;
   }

    // Method to check if a tag is a stop tag
    private static boolean isStopTag(String tag, String[] stopTags) {
        for (String stopTag : stopTags) {
            if (tag.equals(stopTag)) {
                return true;
            }
        }
        return false;
   }

   public static void subscribe(String keyword,MessageObserver o){
       listeners.put(keyword, o);
   }

}
