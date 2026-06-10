# DI12010 Social Network Project

这是一个 Java 社交网络项目，包含控制台版本和 GUI 版本。系统使用图结构表示用户和好友关系，并支持文件读取、文件保存、好友搜索、好友推荐、添加好友的好友、帖子和点赞等功能。

详细数据结构图表见 [DATA_STRUCTURE.md](DATA_STRUCTURE.md)。

## 已完成功能

1. 从 `data/network.txt` 读取完整社交网络。
2. 保存用户、好友关系、帖子和点赞到 `data/network.txt`。
3. 每个用户包含 `id`、`name`、`hometown`、`workplace`。
4. 用户之间可以建立双向好友关系。
5. 查看和编辑当前用户资料。
6. 显示当前用户好友列表。
7. 选择好友并查看该好友资料。
8. 查看某个好友的好友列表。
9. 从好友的好友中添加新好友。
10. 显示当前用户和某个好友的共同好友。
11. 按相同家乡筛选好友。
12. 按相同工作地点筛选好友。
13. 按姓名关键字搜索好友。
14. 根据 friends-of-friends、相同家乡或相同工作地点生成好友推荐。
15. 创建帖子。
16. 查看自己的帖子和好友的帖子。
17. 给可见帖子点赞。
18. 取消点赞。
19. GUI 版本支持以上主要功能。
20. 控制台版本仍然保留，方便备用演示。

## 项目结构

```text
src/
  initial.java
  socialmedia/
    Main.java                 控制台入口
    GuiMain.java              GUI 入口
    SocialNetworkGui.java     Swing 图形界面
    NetworkBootstrap.java     共享启动和示例数据逻辑
    User.java                 用户类
    Post.java                 帖子类
    SocialNetwork.java        核心数据结构和业务逻辑
    NetworkFileManager.java   文件读取和保存
    Menu.java                 控制台菜单
data/
  network.txt
DATA_STRUCTURE.md
README.md
```

## IntelliJ IDEA 运行方式

GUI 版本：

1. 用 IntelliJ IDEA 打开 `D:\slayers_project`。
2. 确认 `src` 是 Sources Root。
3. 打开 `src/socialmedia/GuiMain.java`。
4. 点击 `main` 方法旁边的运行按钮。

控制台版本：

1. 打开 `src/socialmedia/Main.java`。
2. 点击 `main` 方法旁边的运行按钮。

## 终端运行方式

先编译：

```powershell
javac -encoding UTF-8 -d out src/socialmedia/*.java
```

运行 GUI：

```powershell
java -cp out socialmedia.GuiMain
```

运行控制台：

```powershell
java -cp out socialmedia.Main
```

## 创建 runnable JAR

GUI JAR：

```powershell
jar --create --file slayers-social-network-gui.jar --main-class socialmedia.GuiMain -C out .
java -jar slayers-social-network-gui.jar
```

控制台 JAR：

```powershell
jar --create --file slayers-social-network.jar --main-class socialmedia.Main -C out .
java -jar slayers-social-network.jar
```

## 文件格式说明

```text
USER|userId|name|hometown|workplace
FRIENDS|userId|friendId1,friendId2,friendId3
POST|postId|authorId|content
LIKES|postId|userId1,userId2,userId3
```

加载顺序：

1. 先读取 `USER` 行，创建所有用户。
2. 再读取 `FRIENDS` 行，建立双向好友关系。
3. 再读取 `POST` 行，创建帖子。
4. 最后读取 `LIKES` 行，建立点赞关系。

## 核心数据结构

`SocialNetwork` 中使用：

```java
Map<String, User> users
Map<String, Post> posts
String currentUserId
```

`User` 中使用：

```java
Set<String> friendIds
```

`Post` 中使用：

```java
Set<String> likedByUserIds
```

主要选择：

- `HashMap<String, User>`：通过用户 ID 快速查找用户。
- `HashMap<String, Post>`：通过帖子 ID 快速查找帖子。
- `HashSet<String>`：保存好友 ID 和点赞用户 ID，避免重复，并支持快速查找。
- `ArrayList` / `List`：用于返回和显示排序后的好友、帖子、推荐结果。
- Swing 组件：`JFrame`、`JTabbedPane`、`JList`、`JTextField`、`JTextArea`、`JComboBox`、`JButton`。

## 手动测试清单

1. GUI 可以启动并显示当前用户。
2. 可以切换当前用户。
3. 可以编辑并保存个人资料。
4. 可以显示全部好友。
5. 可以按姓名搜索好友。
6. 可以按家乡和工作地点筛选好友。
7. 可以查看选中好友的好友列表。
8. 可以添加好友的好友。
9. 可以显示共同好友。
10. 可以显示好友推荐。
11. 可以创建帖子。
12. 可以查看自己的帖子和好友帖子。
13. 可以点赞和取消点赞。
14. 可以保存网络。
15. 重新运行后，用户、好友、帖子和点赞仍然保留。
