import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a user in the social network.
 * Each user has a profile (userId, name, workplace, hometown),
 * a list of friends (connections), and a list of posts.
 */
public class User {

    /** Unique identifier for the user */
    private String userId;

    /** Display name of the user */
    private String name;

    /** Current workplace of the user */
    private String workplace;

    /** Hometown of the user */
    private String hometown;

    /** Password for authentication */
    private String password;

    /** Personal signature / bio */
    private String signature;

    /** Path to avatar image file */
    private String avatarPath;

    /** List of friends (bidirectional connections) */
    private List<User> friends;

    /** List of posts made by this user */
    private List<Post> posts;

    /**
     * Constructs a new User with the given profile details.
     *
     * @param userId    unique identifier
     * @param name      display name
     * @param workplace current workplace
     * @param hometown  hometown
     */
    public User(String userId, String name, String workplace, String hometown) {
        this(userId, name, workplace, hometown, "");
    }

    /**
     * Constructs a new User with the given profile details and password.
     *
     * @param userId    unique identifier
     * @param name      display name
     * @param workplace current workplace
     * @param hometown  hometown
     * @param password  account password
     */
    public User(String userId, String name, String workplace, String hometown, String password) {
        this.userId = userId;
        this.name = name;
        this.workplace = workplace;
        this.hometown = hometown;
        this.password = password;
        this.friends = new ArrayList<>();
        this.posts = new ArrayList<>();
    }

    // ---- Getters and Setters ----

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorkplace() {
        return workplace;
    }

    public void setWorkplace(String workplace) {
        this.workplace = workplace;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSignature() {
        return signature != null ? signature : "";
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAvatarPath() {
        return avatarPath != null ? avatarPath : "";
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public List<User> getFriends() {
        return friends;
    }

    public List<Post> getPosts() {
        return posts;
    }

    // ---- Friend Management ----

    /**
     * Adds a friend to this user's friend list.
     * Also adds this user to the friend's list (bidirectional).
     *
     * @param friend the user to add as a friend
     */
    public void addFriend(User friend) {
        if (friend != null && !friends.contains(friend) && !friend.equals(this)) {
            friends.add(friend);
            friend.friends.add(this);
        }
    }

    /**
     * Removes a friend from this user's friend list.
     * Also removes this user from the friend's list (bidirectional).
     *
     * @param friend the user to remove
     */
    public void removeFriend(User friend) {
        if (friend != null) {
            friends.remove(friend);
            friend.friends.remove(this);
        }
    }

    /**
     * Checks if the given user is a friend of this user.
     *
     * @param other the user to check
     * @return true if they are friends
     */
    public boolean isFriendWith(User other) {
        return friends.contains(other);
    }

    /**
     * Returns a list of mutual friends between this user and another user.
     *
     * @param other the other user
     * @return list of mutual friends
     */
    public List<User> getMutualFriends(User other) {
        List<User> mutual = new ArrayList<>();
        for (User f : this.friends) {
            if (other.friends.contains(f)) {
                mutual.add(f);
            }
        }
        return mutual;
    }

    // ---- Post Management ----

    /**
     * Adds a post to this user's post list.
     *
     * @param post the post to add
     */
    public void addPost(Post post) {
        if (post != null) {
            posts.add(post);
        }
    }

    // ---- Display ----

    /**
     * Returns a formatted string of the user's profile.
     *
     * @return profile string
     */
    public String getProfileString() {
        return "ID: " + userId + " | Name: " + name
                + " | Workplace: " + workplace
                + " | Hometown: " + hometown;
    }

    @Override
    public String toString() {
        return name + " (" + userId + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
