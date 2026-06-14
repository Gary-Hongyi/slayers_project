import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Handles saving and loading the social network to/from a text file.
 *
 * File format:
 *   [USERS]
 *   userId|name|workplace|hometown|password|signature|avatarPath|friendId:encodedRemark;...|encodedFriendNotification;...
 *   ...
 *   [FRIENDSHIPS]
 *   userId1|userId2
 *   ...
 *   [POSTS]
 *   postId|authorId|content|timestamp|likerId1,likerId2,...
 *   ...
 *   [COMMENTS]
 *   postId|authorId|content|timestamp
 *   ...
 *   [COUNTER]
 *   postCounter
 *   [END]
 */
public class FileManager {

    private static final DateTimeFormatter FORMATTER = Post.getFormatter();

    /**
     * Saves the entire social network to a file.
     *
     * @param filepath the path to save to
     * @param network  the social network to save
     * @throws IOException if writing fails
     */
    public static void saveNetwork(String filepath, SocialNetwork network) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath))) {
            // Write users section
            writer.write("[USERS]");
            writer.newLine();
            for (User user : network.getAllUsers()) {
                writer.write(escape(user.getUserId()) + "|"
                        + escape(user.getName()) + "|"
                        + escape(user.getWorkplace()) + "|"
                        + escape(user.getHometown()) + "|"
                        + escape(user.getPassword()) + "|"
                        + escape(user.getSignature()) + "|"
                        + escape(user.getAvatarPath()) + "|"
                        + escape(serializeRemarks(user)) + "|"
                        + escape(serializeFriendNotifications(user)));
                writer.newLine();
            }

            // Write friendships section (avoid duplicates by only writing userId1 < userId2)
            writer.write("[FRIENDSHIPS]");
            writer.newLine();
            List<String> written = new ArrayList<>();
            for (User user : network.getAllUsers()) {
                for (User friend : user.getFriends()) {
                    String key = user.getUserId().compareTo(friend.getUserId()) < 0
                            ? user.getUserId() + "|" + friend.getUserId()
                            : friend.getUserId() + "|" + user.getUserId();
                    if (!written.contains(key)) {
                        writer.write(escape(user.getUserId()) + "|" + escape(friend.getUserId()));
                        writer.newLine();
                        written.add(key);
                    }
                }
            }

            // Write posts section
            writer.write("[POSTS]");
            writer.newLine();
            for (User user : network.getAllUsers()) {
                for (Post post : user.getPosts()) {
                    writer.write(escape(post.getPostId()) + "|"
                            + escape(post.getAuthor().getUserId()) + "|"
                            + escape(post.getContent()) + "|"
                            + escape(post.getTimestampString()) + "|"
                            + escape(post.getLikerIdsString()));
                    writer.newLine();
                }
            }

            // Write comments section
            writer.write("[COMMENTS]");
            writer.newLine();
            for (User user : network.getAllUsers()) {
                for (Post post : user.getPosts()) {
                    for (Comment comment : post.getComments()) {
                        writer.write(escape(post.getPostId()) + "|"
                                + escape(comment.getAuthor().getUserId()) + "|"
                                + escape(comment.getContent()) + "|"
                                + escape(comment.getTimestampString()));
                        writer.newLine();
                    }
                }
            }

            // Write counter
            writer.write("[COUNTER]");
            writer.newLine();
            writer.write(String.valueOf(network.getPostCounter()));
            writer.newLine();

            writer.write("[END]");
            writer.newLine();
        }
    }

    /**
     * Loads a social network from a file.
     *
     * @param filepath the path to load from
     * @return the reconstructed SocialNetwork
     * @throws IOException if reading fails
     */
    public static SocialNetwork loadNetwork(String filepath) throws IOException {
        SocialNetwork network = new SocialNetwork();
        String section = "";

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Check for section headers
                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line;
                    continue;
                }

                switch (section) {
                    case "[USERS]":
                        parseUser(line, network);
                        break;
                    case "[FRIENDSHIPS]":
                        parseFriendship(line, network);
                        break;
                    case "[POSTS]":
                        parsePost(line, network);
                        break;
                    case "[COMMENTS]":
                        parseComment(line, network);
                        break;
                    case "[COUNTER]":
                        try {
                            network.setPostCounter(Integer.parseInt(line));
                        } catch (NumberFormatException e) {
                            // ignore bad counter
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return network;
    }

    /**
     * Parses a user line and adds the user to the network.
     */
    private static void parseUser(String line, SocialNetwork network) {
        String[] parts = line.split("\\|", -1);
        if (parts.length >= 4) {
            String userId = unescape(parts[0]);
            String name = unescape(parts[1]);
            String workplace = unescape(parts[2]);
            String hometown = unescape(parts[3]);
            String password = parts.length >= 5 ? unescape(parts[4]) : "";
            User user = new User(userId, name, workplace, hometown, password);
            if (parts.length >= 6) user.setSignature(unescape(parts[5]));
            if (parts.length >= 7) user.setAvatarPath(unescape(parts[6]));
            if (parts.length >= 8) parseRemarks(unescape(parts[7]), user);
            if (parts.length >= 9) parseFriendNotifications(unescape(parts[8]), user);
            network.addUser(user);
        }
    }

    /**
     * Parses a friendship line and adds the bidirectional friendship.
     */
    private static void parseFriendship(String line, SocialNetwork network) {
        String[] parts = line.split("\\|", -1);
        if (parts.length >= 2) {
            String id1 = unescape(parts[0]);
            String id2 = unescape(parts[1]);
            network.addFriendship(id1, id2);
        }
    }

    /**
     * Parses a post line and adds it to the network.
     */
    private static void parsePost(String line, SocialNetwork network) {
        String[] parts = line.split("\\|", -1);
        if (parts.length >= 5) {
            String postId = unescape(parts[0]);
            String authorId = unescape(parts[1]);
            String content = unescape(parts[2]);
            String timestampStr = unescape(parts[3]);
            String likerIds = unescape(parts[4]);

            User author = network.getUser(authorId);
            if (author == null) return;

            LocalDateTime timestamp;
            try {
                timestamp = LocalDateTime.parse(timestampStr, FORMATTER);
            } catch (Exception e) {
                timestamp = LocalDateTime.now();
            }

            Post post = new Post(postId, author, content, timestamp);
            author.addPost(post);

            // Add likes
            if (!likerIds.isEmpty()) {
                String[] ids = likerIds.split(",");
                for (String id : ids) {
                    id = id.trim();
                    if (!id.isEmpty()) {
                        User liker = network.getUser(id);
                        if (liker != null) {
                            post.addLike(liker);
                        }
                    }
                }
            }
        }
    }

    /**
     * Escapes pipe characters in data for safe serialization.
     */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("|", "\\p");
    }

    /**
     * Unescapes pipe characters from serialized data.
     */
    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\p", "|").replace("\\\\", "\\");
    }

    private static String serializeRemarks(User user) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : user.getFriendRemarks().entrySet()) {
            if (entry.getValue() == null || entry.getValue().trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(encode(entry.getValue()));
        }
        return sb.toString();
    }

    private static void parseRemarks(String data, User user) {
        if (data == null || data.trim().isEmpty()) return;
        String[] entries = data.split(";");
        for (String entry : entries) {
            int sep = entry.indexOf(':');
            if (sep <= 0 || sep >= entry.length() - 1) continue;
            String friendId = entry.substring(0, sep);
            String remark = decode(entry.substring(sep + 1));
            user.setFriendRemark(friendId, remark);
        }
    }

    private static String serializeFriendNotifications(User user) {
        StringBuilder sb = new StringBuilder();
        for (String userId : user.getFriendNotifications()) {
            if (userId == null || userId.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append(encode(userId.trim()));
        }
        return sb.toString();
    }

    private static void parseFriendNotifications(String data, User user) {
        if (data == null || data.trim().isEmpty() || user == null) return;
        String[] entries = data.split(";");
        for (String entry : entries) {
            String userId = decode(entry);
            if (!userId.isEmpty()) user.addFriendNotification(userId);
        }
    }

    /**
     * Parses a comment line and attaches it to the matching post.
     */
    private static void parseComment(String line, SocialNetwork network) {
        String[] parts = line.split("\\|", -1);
        if (parts.length >= 4) {
            String postId = unescape(parts[0]);
            String authorId = unescape(parts[1]);
            String content = unescape(parts[2]);
            String timestampStr = unescape(parts[3]);

            Post post = network.getPostById(postId);
            User author = network.getUser(authorId);
            if (post == null || author == null) return;

            LocalDateTime timestamp;
            try {
                timestamp = LocalDateTime.parse(timestampStr, FORMATTER);
            } catch (Exception e) {
                timestamp = LocalDateTime.now();
            }
            post.addComment(new Comment(author, content, timestamp));
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
