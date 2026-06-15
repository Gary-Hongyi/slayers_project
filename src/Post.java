import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a post in the social network.
 * Each post has an author, content, timestamp, and a list of users who liked it.
 */
public class Post {

    /** Unique identifier for the post */
    private String postId;

    /** The user who created this post */
    private User author;

    /** The text content of the post */
    private String content;

    /** Timestamp of when the post was created */
    private LocalDateTime timestamp;

    /** List of users who liked this post */
    private List<User> likes;

    /** List of comments on this post */
    private List<Comment> comments;

    /** Date format for display and serialization */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructs a new Post.
     *
     * @param postId  unique identifier
     * @param author  the user who wrote this post
     * @param content the text content
     */
    public Post(String postId, User author, String content) {
        this.postId = postId;
        this.author = author;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.likes = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    /**
     * Constructs a Post with a specific timestamp (for loading from file).
     *
     * @param postId    unique identifier
     * @param author    the user who wrote this post
     * @param content   the text content
     * @param timestamp the creation timestamp
     */
    public Post(String postId, User author, String content, LocalDateTime timestamp) {
        this.postId = postId;
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
        this.likes = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    // ---- Getters ----

    public String getPostId() {
        return postId;
    }

    public User getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTimestampString() {
        return timestamp.format(FORMATTER);
    }

    public List<User> getLikes() {
        return likes;
    }

    public int getLikeCount() {
        return likes.size();
    }

    public List<Comment> getComments() {
        return comments;
    }

    public int getCommentCount() {
        return comments.size();
    }

    // ---- Like Management ----

    /**
     * Adds a like from the given user.
     *
     * @param user the user who likes this post
     */
    public void addLike(User user) {
        if (user != null && !likes.contains(user)) {
            likes.add(user);
        }
    }

    /**
     * Removes a like from the given user.
     *
     * @param user the user who unliked this post
     */
    public void removeLike(User user) {
        likes.remove(user);
    }

    /**
     * Checks if a user has liked this post.
     *
     * @param user the user to check
     * @return true if the user liked this post
     */
    public boolean isLikedBy(User user) {
        return likes.contains(user);
    }

    /**
     * Returns the list of liker user IDs for serialization.
     *
     * @return comma-separated string of liker IDs
     */
    public String getLikerIdsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < likes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(likes.get(i).getUserId());
        }
        return sb.toString();
    }

    // ---- Comment Management ----

    public Comment addComment(User author, String content) {
        if (author == null || content == null || content.trim().isEmpty()) return null;
        Comment comment = new Comment(author, content.trim());
        comments.add(comment);
        return comment;
    }

    public void addComment(Comment comment) {
        if (comment != null) {
            comments.add(comment);
        }
    }

    @Override
    public String toString() {
        return author.getName() + ": \"" + content + "\" [" + getTimestampString() + "] "
                + "Likes: " + likes.size() + " Comments: " + comments.size();
    }

    public static DateTimeFormatter getFormatter() {
        return FORMATTER;
    }
}
