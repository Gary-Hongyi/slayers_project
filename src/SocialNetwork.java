import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the entire social network as an undirected graph.
 * Uses a HashMap for O(1) user lookup by userId.
 * Friendships are stored as bidirectional edges between User nodes.
 */
public class SocialNetwork {

    /** Map of all users indexed by userId for O(1) lookup */
    private HashMap<String, User> users;

    /** The currently logged-in user */
    private User currentUser;

    /** Counter for generating unique post IDs */
    private int postCounter;

    /**
     * Constructs an empty SocialNetwork.
     */
    public SocialNetwork() {
        this.users = new HashMap<>();
        this.currentUser = null;
        this.postCounter = 0;
    }

    // ---- User Management ----

    /**
     * Adds a user to the network.
     *
     * @param user the user to add
     */
    public void addUser(User user) {
        if (user != null && !users.containsKey(user.getUserId())) {
            users.put(user.getUserId(), user);
        }
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId the user ID
     * @return the User, or null if not found
     */
    public User getUser(String userId) {
        return users.get(userId);
    }

    /**
     * Returns all users in the network.
     *
     * @return collection of all users
     */
    public Collection<User> getAllUsers() {
        return users.values();
    }

    /**
     * Returns the number of users in the network.
     *
     * @return user count
     */
    public int getUserCount() {
        return users.size();
    }

    // ---- Current User ----

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    // ---- Friendship Management ----

    /**
     * Creates a friendship between two users (bidirectional).
     *
     * @param userId1 first user ID
     * @param userId2 second user ID
     */
    public void addFriendship(String userId1, String userId2) {
        User u1 = users.get(userId1);
        User u2 = users.get(userId2);
        if (u1 != null && u2 != null) {
            u1.addFriend(u2);
        }
    }

    // ---- Queries ----

    /**
     * Returns mutual friends between two users.
     *
     * @param user1 first user
     * @param user2 second user
     * @return list of mutual friends
     */
    public List<User> getMutualFriends(User user1, User user2) {
        if (user1 == null || user2 == null) return new ArrayList<>();
        return user1.getMutualFriends(user2);
    }

    /**
     * Filters a user's friends by hometown.
     *
     * @param user     the user whose friends to filter
     * @param hometown the hometown to match (case-insensitive)
     * @return list of friends with matching hometown
     */
    public List<User> filterFriendsByHometown(User user, String hometown) {
        if (user == null || hometown == null) return new ArrayList<>();
        return user.getFriends().stream()
                .filter(f -> f.getHometown().equalsIgnoreCase(hometown))
                .collect(Collectors.toList());
    }

    /**
     * Filters a user's friends by workplace.
     *
     * @param user      the user whose friends to filter
     * @param workplace the workplace to match (case-insensitive)
     * @return list of friends with matching workplace
     */
    public List<User> filterFriendsByWorkplace(User user, String workplace) {
        if (user == null || workplace == null) return new ArrayList<>();
        return user.getFriends().stream()
                .filter(f -> f.getWorkplace().equalsIgnoreCase(workplace))
                .collect(Collectors.toList());
    }

    /**
     * Generates friend recommendations for the current user.
     * Scans friends-of-friends and finds people with the same hometown or workplace.
     *
     * @param user the user to generate recommendations for
     * @return map of recommended users to the reason for recommendation
     */
    public Map<User, String> getFriendRecommendations(User user) {
        Map<User, String> recommendations = new LinkedHashMap<>();
        if (user == null) return recommendations;

        Set<User> friendsSet = new HashSet<>(user.getFriends());

        // Look through all friends' friends
        for (User friend : user.getFriends()) {
            for (User fof : friend.getFriends()) {
                // Skip self and existing friends
                if (fof.equals(user) || friendsSet.contains(fof)) continue;

                String reason = "";
                if (fof.getHometown().equalsIgnoreCase(user.getHometown())) {
                    reason += "Same hometown (" + user.getHometown() + ")";
                }
                if (fof.getWorkplace().equalsIgnoreCase(user.getWorkplace())) {
                    if (!reason.isEmpty()) reason += "; ";
                    reason += "Same workplace (" + user.getWorkplace() + ")";
                }

                // Also check mutual friends
                if (reason.isEmpty()) {
                    List<User> mutual = user.getMutualFriends(fof);
                    if (!mutual.isEmpty()) {
                        reason = mutual.size() + " mutual friend(s)";
                    }
                }

                if (!reason.isEmpty()) {
                    recommendations.put(fof, reason);
                }
            }
        }
        return recommendations;
    }

    // ---- Post Management ----

    /**
     * Creates a new post for the given user and returns it.
     *
     * @param user    the author
     * @param content the post content
     * @return the created Post
     */
    public Post createPost(User user, String content) {
        postCounter++;
        String postId = "P" + postCounter;
        Post post = new Post(postId, user, content);
        user.addPost(post);
        return post;
    }

    /**
     * Returns the next post counter value (for serialization).
     */
    public int getPostCounter() {
        return postCounter;
    }

    /**
     * Sets the post counter (for deserialization).
     */
    public void setPostCounter(int postCounter) {
        this.postCounter = postCounter;
    }

    /**
     * Collects all posts from the network for display.
     *
     * @return list of all posts sorted by timestamp (newest first)
     */
    public List<Post> getAllPosts() {
        List<Post> allPosts = new ArrayList<>();
        for (User user : users.values()) {
            allPosts.addAll(user.getPosts());
        }
        allPosts.sort((p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
        return allPosts;
    }
}
