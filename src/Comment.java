import java.time.LocalDateTime;

/**
 * Represents a comment on a post.
 * Each comment stores the author, text content, and creation time.
 */
public class Comment {

    private User author;
    private String content;
    private LocalDateTime timestamp;

    public Comment(User author, String content) {
        this(author, content, LocalDateTime.now());
    }

    public Comment(User author, String content, LocalDateTime timestamp) {
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
    }

    public User getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTimestampString() {
        return timestamp.format(Post.getFormatter());
    }
}
