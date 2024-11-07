package chatclient.responses;

/**
 * Class used by Gson when parsing JSON responses
 * for representing a Message.
 */
public class Message {
    private int id;
    private String text;
    private int room_id;
    private int user_id;
    private String created_at;
    private String username;
    private String eventType;

    public Message(int id, String text, int room_id, int user_id, String created_at, String username, String eventType) {
        this.id = id;
        this.text = text;
        this.room_id = room_id;
        this.user_id = user_id;
        this.created_at = created_at;
        this.username = username;
        this.eventType = eventType;
    }

    public String getEventType() {
        return text;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public int getRoom_id() {
        return room_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getUsername() {
        return this.username;
    }
}
