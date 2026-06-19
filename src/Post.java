import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a social media post with content, timestamp, likes and comments.
 *
 * @author Team Slayers
 * @version 1.0
 */
public class Post {

    private String postId;

    private User author;

    private String content;

    private LocalDateTime timestamp;

    private List<User> likes;

    private List<Comment> comments;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructs a new Post object.
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
     * Constructs a new Post object.
     */
    public Post(String postId, User author, String content, LocalDateTime timestamp) {
        this.postId = postId;
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
        this.likes = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    /**
     * Returns the post id.
     */
    public String getPostId() {
        return postId;
    }

    /**
     * Returns the author.
     */
    public User getAuthor() {
        return author;
    }

    /**
     * Returns the content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns the timestamp.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the timestamp string.
     */
    public String getTimestampString() {
        return timestamp.format(FORMATTER);
    }

    /**
     * Returns the likes.
     */
    public List<User> getLikes() {
        return likes;
    }

    /**
     * Returns the like count.
     */
    public int getLikeCount() {
        return likes.size();
    }

    /**
     * Returns the comments.
     */
    public List<Comment> getComments() {
        return comments;
    }

    /**
     * Returns the comment count.
     */
    public int getCommentCount() {
        return comments.size();
    }

    /**
     * Adds the like.
     */
    public void addLike(User user) {
        if (user != null && !likes.contains(user)) {
            likes.add(user);
        }
    }

    /**
     * Removes the like.
     */
    public void removeLike(User user) {
        likes.remove(user);
    }

    /**
     * Checks whether is liked by.
     */
    public boolean isLikedBy(User user) {
        return likes.contains(user);
    }

    /**
     * Returns the liker ids string.
     */
    public String getLikerIdsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < likes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(likes.get(i).getUserId());
        }
        return sb.toString();
    }

    /**
     * Adds the comment.
     */
    public Comment addComment(User author, String content) {
        if (author == null || content == null || content.trim().isEmpty()) return null;
        Comment comment = new Comment(author, content.trim());
        comments.add(comment);
        return comment;
    }

    /**
     * Adds the comment.
     */
    public void addComment(Comment comment) {
        if (comment != null) {
            comments.add(comment);
        }
    }

    /**
     * Returns a readable text representation of the object.
     */
    @Override
    public String toString() {
        return author.getName() + ": \"" + content + "\" [" + getTimestampString() + "] "
                + "Likes: " + likes.size() + " Comments: " + comments.size();
    }

    /**
     * Returns the formatter.
     */
    public static DateTimeFormatter getFormatter() {
        return FORMATTER;
    }
}
