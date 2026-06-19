import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a social network user with profile information, friends, posts and notifications.
 *
 * @author Team Slayers
 * @version 1.0
 */
public class User {

    private String userId;

    private String name;

    private String workplace;

    private String hometown;

    private String password;

    private String signature;

    private String avatarPath;

    private List<User> friends;

    private List<Post> posts;

    private Map<String, String> friendRemarks;

    private List<String> friendNotifications;

    /**
     * Constructs a new User object.
     */
    public User(String userId, String name, String workplace, String hometown) {
        this(userId, name, workplace, hometown, "");
    }

    /**
     * Constructs a new User object.
     */
    public User(String userId, String name, String workplace, String hometown, String password) {
        this.userId = userId;
        this.name = name;
        this.workplace = workplace;
        this.hometown = hometown;
        this.password = password;
        this.friends = new ArrayList<>();
        this.posts = new ArrayList<>();
        this.friendRemarks = new HashMap<>();
        this.friendNotifications = new ArrayList<>();
    }

    /**
     * Returns the user id.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the workplace.
     */
    public String getWorkplace() {
        return workplace;
    }

    /**
     * Sets the workplace.
     */
    public void setWorkplace(String workplace) {
        this.workplace = workplace;
    }

    /**
     * Returns the hometown.
     */
    public String getHometown() {
        return hometown;
    }

    /**
     * Sets the hometown.
     */
    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    /**
     * Returns the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the signature.
     */
    public String getSignature() {
        return signature != null ? signature : "";
    }

    /**
     * Sets the signature.
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * Returns the avatar path.
     */
    public String getAvatarPath() {
        return avatarPath != null ? avatarPath : "";
    }

    /**
     * Sets the avatar path.
     */
    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    /**
     * Returns the friends.
     */
    public List<User> getFriends() {
        return friends;
    }

    /**
     * Returns the posts.
     */
    public List<Post> getPosts() {
        return posts;
    }

    /**
     * Returns the friend remarks.
     */
    public Map<String, String> getFriendRemarks() {
        return friendRemarks;
    }

    /**
     * Returns the friend remark.
     */
    public String getFriendRemark(String friendId) {
        if (friendId == null) return "";
        String remark = friendRemarks.get(friendId);
        return remark != null ? remark : "";
    }

    /**
     * Sets the friend remark.
     */
    public void setFriendRemark(String friendId, String remark) {
        if (friendId == null || friendId.trim().isEmpty()) return;
        String clean = remark != null ? remark.trim() : "";
        if (clean.isEmpty()) {
            friendRemarks.remove(friendId);
        } else {
            friendRemarks.put(friendId, clean);
        }
    }

    /**
     * Returns the display name for.
     */
    public String getDisplayNameFor(User friend) {
        if (friend == null) return "";
        String remark = getFriendRemark(friend.getUserId());
        return remark.isEmpty() ? friend.getName() : remark;
    }

    /**
     * Returns the friend notifications.
     */
    public List<String> getFriendNotifications() {
        return friendNotifications;
    }

    /**
     * Adds the friend notification.
     */
    public void addFriendNotification(String fromUserId) {
        if (fromUserId == null) return;
        String clean = fromUserId.trim();
        if (clean.isEmpty() || clean.equals(userId) || friendNotifications.contains(clean)) return;
        friendNotifications.add(clean);
    }

    /**
     * Clears the friend notifications.
     */
    public void clearFriendNotifications() {
        friendNotifications.clear();
    }

    /**
     * Adds the friend.
     */
    public void addFriend(User friend) {
        if (friend != null && !friends.contains(friend) && !friend.equals(this)) {
            friends.add(friend);
            friend.friends.add(this);
        }
    }

    /**
     * Removes the friend.
     */
    public void removeFriend(User friend) {
        if (friend != null) {
            friends.remove(friend);
            friend.friends.remove(this);
            friendRemarks.remove(friend.getUserId());
            friend.friendRemarks.remove(this.getUserId());
        }
    }

    /**
     * Checks whether is friend with.
     */
    public boolean isFriendWith(User other) {
        return friends.contains(other);
    }

    /**
     * Returns the mutual friends.
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

    /**
     * Adds the post.
     */
    public void addPost(Post post) {
        if (post != null) {
            posts.add(post);
        }
    }

    /**
     * Removes the post.
     */
    public boolean removePost(Post post) {
        if (post == null) return false;
        String postId = post.getPostId();
        return posts.removeIf(existing -> existing == post
                || (existing != null && Objects.equals(existing.getPostId(), postId)));
    }

    /**
     * Returns the profile string.
     */
    public String getProfileString() {
        return "ID: " + userId + " | Name: " + name
                + " | Workplace: " + workplace
                + " | Hometown: " + hometown;
    }

    /**
     * Returns a readable text representation of the object.
     */
    @Override
    public String toString() {
        return name + " (" + userId + ")";
    }

    /**
     * Checks whether this object is equal to another object.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    /**
     * Returns the hash code used by hash-based collections.
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
