import java.time.LocalDateTime;

/**
 * Represents a comment written by a user on a post.
 *
 * @author Team Slayers
 * @version 1.0
 */
public class Comment {

    private User author;
    private String content;
    private LocalDateTime timestamp;

    /**
     * Constructs a new Comment object.
     */
    public Comment(User author, String content) {
        this(author, content, LocalDateTime.now());
    }

    /**
     * Constructs a new Comment object.
     */
    public Comment(User author, String content, LocalDateTime timestamp) {
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
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
     * Returns the timestamp.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the timestamp string.
     */
    public String getTimestampString() {
        return timestamp.format(Post.getFormatter());
    }
}
