import java.util.*;
import java.util.stream.Collectors;

/**
 * Stores users and provides friendship, recommendation and post operations.
 *
 * @author Team Slayers
 * @version 1.0
 */
public class SocialNetwork {

    private HashMap<String, User> users;

    private User currentUser;

    private int postCounter;

    /**
     * Constructs a new SocialNetwork object.
     */
    public SocialNetwork() {
        this.users = new HashMap<>();
        this.currentUser = null;
        this.postCounter = 0;
    }

    /**
     * Adds the user.
     */
    public void addUser(User user) {
        if (user != null && !users.containsKey(user.getUserId())) {
            users.put(user.getUserId(), user);
        }
    }

    /**
     * Returns the user.
     */
    public User getUser(String userId) {
        return users.get(userId);
    }

    /**
     * Returns the all users.
     */
    public Collection<User> getAllUsers() {
        return users.values();
    }

    /**
     * Returns the user count.
     */
    public int getUserCount() {
        return users.size();
    }

    /**
     * Returns the current user.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the current user.
     */
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Adds the friendship.
     */
    public void addFriendship(String userId1, String userId2) {
        User u1 = users.get(userId1);
        User u2 = users.get(userId2);
        if (u1 != null && u2 != null) {
            u1.addFriend(u2);
        }
    }

    /**
     * Returns the mutual friends.
     */
    public List<User> getMutualFriends(User user1, User user2) {
        if (user1 == null || user2 == null) return new ArrayList<>();
        return user1.getMutualFriends(user2);
    }

    /**
     * Handles the filter friends by hometown operation.
     */
    public List<User> filterFriendsByHometown(User user, String hometown) {
        if (user == null || hometown == null) return new ArrayList<>();
        return user.getFriends().stream()
                .filter(f -> f.getHometown().equalsIgnoreCase(hometown))
                .collect(Collectors.toList());
    }

    /**
     * Handles the filter friends by workplace operation.
     */
    public List<User> filterFriendsByWorkplace(User user, String workplace) {
        if (user == null || workplace == null) return new ArrayList<>();
        return user.getFriends().stream()
                .filter(f -> f.getWorkplace().equalsIgnoreCase(workplace))
                .collect(Collectors.toList());
    }

    /**
     * Returns the friend recommendations.
     */
    public Map<User, String> getFriendRecommendations(User user) {
        Map<User, String> recommendations = new LinkedHashMap<>();
        if (user == null) return recommendations;

        Set<User> friendsSet = new HashSet<>(user.getFriends());

        for (User friend : user.getFriends()) {
            for (User fof : friend.getFriends()) {

                if (fof.equals(user) || friendsSet.contains(fof)) continue;

                String reason = "";
                if (profileValueMatches(fof.getHometown(), user.getHometown())) {
                    reason += "Same hometown (" + user.getHometown() + ")";
                }
                if (profileValueMatches(fof.getWorkplace(), user.getWorkplace())) {
                    if (!reason.isEmpty()) reason += "; ";
                    reason += "Same workplace (" + user.getWorkplace() + ")";
                }

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

    /**
     * Handles the profile value matches operation.
     */
    private boolean profileValueMatches(String left, String right) {
        if (left == null || right == null) return false;
        String leftClean = left.trim();
        String rightClean = right.trim();
        if (leftClean.isEmpty() || rightClean.isEmpty()) return false;
        if ("Unknown".equalsIgnoreCase(leftClean) || "Unknown".equalsIgnoreCase(rightClean)) return false;
        return leftClean.equalsIgnoreCase(rightClean);
    }

    /**
     * Creates the post.
     */
    public Post createPost(User user, String content) {
        postCounter++;
        String postId = "P" + postCounter;
        Post post = new Post(postId, user, content);
        user.addPost(post);
        return post;
    }

    /**
     * Returns the post counter.
     */
    public int getPostCounter() {
        return postCounter;
    }

    /**
     * Sets the post counter.
     */
    public void setPostCounter(int postCounter) {
        this.postCounter = postCounter;
    }

    /**
     * Returns the all posts.
     */
    public List<Post> getAllPosts() {
        List<Post> allPosts = new ArrayList<>();
        for (User user : users.values()) {
            allPosts.addAll(user.getPosts());
        }
        allPosts.sort((p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
        return allPosts;
    }

    /**
     * Returns the visible posts for.
     */
    public List<Post> getVisiblePostsFor(User viewer) {
        List<Post> visiblePosts = new ArrayList<>();
        if (viewer == null) return visiblePosts;

        for (User user : users.values()) {
            if (user.equals(viewer) || viewer.isFriendWith(user)) {
                visiblePosts.addAll(user.getPosts());
            }
        }
        visiblePosts.sort((p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
        return visiblePosts;
    }

    /**
     * Checks whether can view post.
     */
    public boolean canViewPost(User viewer, Post post) {
        if (viewer == null || post == null || post.getAuthor() == null) return false;
        User author = post.getAuthor();
        return author.equals(viewer) || viewer.isFriendWith(author);
    }

    /**
     * Returns the post by id.
     */
    public Post getPostById(String postId) {
        if (postId == null) return null;
        for (User user : users.values()) {
            for (Post post : user.getPosts()) {
                if (post.getPostId().equals(postId)) {
                    return post;
                }
            }
        }
        return null;
    }
}
