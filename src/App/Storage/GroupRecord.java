package App.Storage;

import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;

public class GroupRecord {
    public final String chatId;
    public final String symmetricKey;
    public final ArrayList<String> members;

    public GroupRecord(ArrayList<String> members, String chatId, String symmetricKey) {
        this.members = members;
        this.chatId = chatId;
        this.symmetricKey = symmetricKey;
    }

    public GroupRecord(ArrayList<String> members, String chatId) throws NoSuchAlgorithmException {
        this.members = members;
        this.chatId = chatId;

        String key = MessagesRepository.mr().getStorageKey(chatId);
        if (key.isEmpty()) {
            this.symmetricKey = Base64.getEncoder().encodeToString(KeyGenerator.getInstance("AES").generateKey().getEncoded());
        } else {
            this.symmetricKey = key;
        }
    }

    public String toStorageString() {
        StringBuilder groupRecord = new StringBuilder(chatId + "@");
        groupRecord.append(symmetricKey);
        for (String member : members) {
            groupRecord.append("@").append(member);
        }
        return String.valueOf(groupRecord);
    }
}
