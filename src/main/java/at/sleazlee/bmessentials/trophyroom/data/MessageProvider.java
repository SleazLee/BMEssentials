package at.sleazlee.bmessentials.trophyroom.data;


import at.sleazlee.bmessentials.trophyroom.db.Database;
import java.sql.SQLException;

public class MessageProvider {
    private String message;

    private MessageProvider(String message) {
        this.message = message;
    }

    public MessageProvider replace(String key, String value) {
        return new MessageProvider(this.message.replace(key, value));
    }

    public String getMessage() {
        return this.message.replace("&", "§");
    }

    public static MessageProvider build(String messageName) {
        try {
            return new MessageProvider(Database.getDatabase().getMessage(messageName));
        } catch (SQLException var2) {
            var2.printStackTrace();
            return null;
        }
    }
}

