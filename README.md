# DI12010 SnapTok Social Network Project

SnapTok is a Java Swing social network application for the DI12010 data structures and algorithms team project. It models users and friendships as a social graph, supports file loading and saving, and includes profile editing, friend filtering, friend recommendations, friend-add reminders, posts, likes, comments, avatars, signatures, and per-user friend remarks.

See [DATA_STRUCTURE.md](DATA_STRUCTURE.md) for the data structures and flow diagrams.

## Current Features

1. User login and account creation.
2. User ID and password validation.
3. Local password reset using profile verification.
4. Avatar selection during registration and profile editing.
5. Profile editing for name, signature, workplace, and hometown.
6. Friend list display.
7. Friend filtering by all friends, same hometown, and same workplace.
8. Friend search by name, user ID, or remark.
9. Friend detail view with profile information and mutual friend count.
10. Viewing a friend's friends.
11. Adding a friend from search results or a friend's friend list.
12. Removing a friend.
13. Login reminder when another user added this account as a friend.
14. Per-user friend remarks.
15. Friend recommendations by mutual friends, same workplace, and same hometown.
16. Creating posts.
17. Viewing only the current user's and friends' posts in the Moments feed.
18. Viewing the current user's own posts in the Profile page.
19. Liking and unliking visible posts.
20. Adding real comments to posts.
21. Saving and loading the full network from a file.

## Project Structure

```text
src/
  SocialMediaApp.java     Application entry point
  MainGUI.java            Borderless main Swing window and card navigation
  LoginPanel.java         Login, registration, password reset, account persistence
  MainContentPanel.java   Main Profile, Friends, Posts, and Search UI
  SocialNetwork.java      Core social graph and query logic
  User.java               User profile, friends, posts, and friend remarks
  Post.java               Post content, likes, and comments
  Comment.java            Comment author, content, and timestamp
  FileManager.java        Full network file loading and saving

data/
  network.txt             Example/default network file

users.txt                 Local account/profile store
DATA_STRUCTURE.md         Data structure and flow documentation
README.md                 Project overview
```

## IntelliJ IDEA Run Instructions

1. Open this `slayers_project` folder in IntelliJ IDEA.
2. Mark `src` as Sources Root if IntelliJ does not do this automatically.
3. Open `src/SocialMediaApp.java`.
4. Run the `main` method.

## Terminal Run Instructions

Compile:

```powershell
javac -encoding UTF-8 -d out src\*.java
```

Run:

```powershell
java -cp out SocialMediaApp
```

Run the automated test plan:

```powershell
java -cp out TestPlan
```

## Build a Runnable JAR

```powershell
javac -encoding UTF-8 -d out src\*.java
jar --create --file slayers-social-network-gui.jar --main-class SocialMediaApp -C out .
java -jar slayers-social-network-gui.jar
```

## Account Store

`users.txt` stores local login and profile data:

```text
userId,password,name,workplace,hometown,signature,avatarPath,friendId:encodedRemark;...,encodedFriendNotification;...
```

Fields are escaped when written so commas in profile text do not break the file format.

## Network File Format

The full network file uses sections:

```text
[USERS]
userId|name|workplace|hometown|password|signature|avatarPath|friendId:encodedRemark;...|encodedFriendNotification;...

[FRIENDSHIPS]
userId1|userId2

[POSTS]
postId|authorId|content|timestamp|likerId1,likerId2,...

[COMMENTS]
postId|authorId|content|timestamp

[COUNTER]
postCounter

[END]
```

Loading order:

1. Read `[USERS]` and create all `User` objects.
2. Read `[FRIENDSHIPS]` and create bidirectional friendships.
3. Read `[POSTS]` and attach posts to authors.
4. Read `[COMMENTS]` and attach comments to posts.
5. Read `[COUNTER]` and restore the next post ID counter.

## Core Data Structures

`SocialNetwork` uses:

```java
HashMap<String, User> users;
User currentUser;
int postCounter;
```

`User` uses:

```java
List<User> friends;
List<Post> posts;
Map<String, String> friendRemarks;
```

`Post` uses:

```java
List<User> likes;
List<Comment> comments;
```

These structures support quick user lookup by ID, graph traversal through friend lists, per-user remarks, and post interactions with likes and comments.

## Manual Test Checklist

1. Register a new account with a valid User ID and password.
2. Choose an avatar during registration.
3. Log in with the new account.
4. Edit and save profile details.
5. Use the Friends page filters.
6. Search friends by name, ID, or remark.
7. Add, remove, and view friends.
8. Edit a friend remark and confirm the list display changes.
9. Use Search recommendations by mutual friends, same workplace, and same hometown.
10. Search users by ID or name.
11. Create a post.
12. Confirm Moments shows only the current user's and friends' posts.
13. Like and unlike a post.
14. Add a comment to a post.
15. Save the full network to a file.
16. Load the full network back and confirm users, friendships, posts, likes, comments, and remarks are restored.
