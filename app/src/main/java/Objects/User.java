package Objects;

public class User {
    private static User instance; // Singleton instance
    private String UID;
    private String username;

    // Private constructor to prevent instantiation
    private User() {}

    // Public method to provide access to the singleton instance
    public static User getInstance() {
        if (instance == null) {
            synchronized (User.class) {
                if (instance == null) {
                    instance = new User();
                }
            }
        }
        return instance;
    }

    // Getters and Setters
    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}


