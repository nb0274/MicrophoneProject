package Objects;

import com.google.firebase.auth.FirebaseAuth;

public class User {
    private static User instance; // Singleton instance
    private String UID;
    private String username;
    private String password;

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
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
