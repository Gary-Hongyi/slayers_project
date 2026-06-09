import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * WeChat-style three-column main content panel.
 * Left: 60px nav icons | Middle: 300px list | Right: detail view.
 */
public class MainContentPanel extends JPanel {

    private MainGUI mainGUI;
    private SocialNetwork network;
    static final Font YH = new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 14);
    static final Font YHB = new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 14);
    static final Color BRAND = new Color(59, 130, 246);
    static final Color BRAND_DARK = new Color(37, 99, 235);
    static final Color TEXT_MAIN = new Color(55, 65, 81);
    static final Color TEXT_SUB = new Color(107, 114, 128);
    static final Color TEXT_HINT = new Color(156, 163, 175);
    static final Color DIVIDER = new Color(229, 231, 235);
    static final Color HOVER_BG = new Color(243, 244, 246);
    static final Color INPUT_BG = new Color(243, 244, 246);

    private int navIdx = 0;
    private CardLayout midCards, rightCards;
    private JPanel midPanel, rightPanel;
    private User detailUser = null;
    private Post detailPost = null;

    // Profile
    private JLabel pAvatar, pName, pId;
    private JTextField pNameF, pWorkF, pHomeF;
    // Friends
    private DefaultListModel<User> fModel;
    private JList<User> fList;
    private JTextField fSearch;
    private String fFilter = "All";
    // Moments
    private JTextArea mInput;
    private JPanel mFeed;
    // Search
    private JTextField sField;
    private DefaultListModel<User> sModel;
    private JList<User> sList;
    // Right detail labels
    private JLabel rAvatar, rName, rId;
    private JPanel rWork, rHome, rSig, rMutual;
    private JLabel rFriends, rPosts;
    private JTextArea rSigArea;
    private JPanel rActionPanel, rExtraPanel;
    private String rightCard = "PROFILE";

    public MainContentPanel(MainGUI mainGUI, SocialNetwork network) {
        this.mainGUI = mainGUI;
        this.network = network;
        setLayout(new BorderLayout());
        setOpaque(false);

        midCards = new CardLayout();
        midPanel = new JPanel(midCards);
        midPanel.setPreferredSize(new Dimension(300, 0));
        midPanel.setOpaque(false);
        midPanel.add(buildProfileMid(), "PROFILE");
        midPanel.add(buildFriendsMid(), "FRIENDS");
        midPanel.add(buildMomentsMid(), "MOMENTS");
        midPanel.add(buildSearchMid(), "SEARCH");

        rightCards = new CardLayout();
        rightPanel = new JPanel(rightCards);
        rightPanel.setOpaque(false);
        rightPanel.add(buildProfileRight(), "PROFILE");
        rightPanel.add(buildDetailRight(), "DETAIL");

        JPanel midWithSep = new JPanel(new BorderLayout());
        midWithSep.setOpaque(false);
        midWithSep.add(midPanel, BorderLayout.CENTER);
        midWithSep.add(new JSep(), BorderLayout.EAST);

        JPanel centerAll = new JPanel(new BorderLayout());
        centerAll.setOpaque(false);
        centerAll.add(midWithSep, BorderLayout.WEST);
        centerAll.add(rightPanel, BorderLayout.CENTER);

        add(buildNav(), BorderLayout.WEST);
        add(centerAll, BorderLayout.CENTER);

        midCards.show(midPanel, "PROFILE");
        rightCards.show(rightPanel, "PROFILE");
    }

    // ======== LEFT NAV ========
    private JPanel buildNav() {
        JPanel nav = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        nav.setPreferredSize(new Dimension(60, 0));
        String[] tips = {"Profile", "Friends", "Moments", "Search"};
        for (int i = 0; i < 4; i++) {
            NavIcon ni = new NavIcon(i, tips[i]);
            ni.setBounds(0, 24 + i * 56, 60, 48);
            nav.add(ni);
        }
        // Save/Load at bottom
        NavIcon saveIcon = new NavIcon(10, "Save Network");
        saveIcon.setBounds(0, 500, 60, 48);
        nav.add(saveIcon);
        NavIcon loadIcon = new NavIcon(11, "Load Network");
        loadIcon.setBounds(0, 548, 60, 48);
        nav.add(loadIcon);
        NavIcon logoutIcon = new NavIcon(12, "Logout");
        logoutIcon.setBounds(0, 596, 60, 48);
        nav.add(logoutIcon);
        return nav;
    }

    private void selectNav(int idx) {
        navIdx = idx;
        repaint();
        switch (idx) {
            case 0: refreshProfile(); midCards.show(midPanel, "PROFILE"); rightCards.show(rightPanel, "PROFILE"); break;
            case 1: refreshFriends(); midCards.show(midPanel, "FRIENDS"); showProfileRight(); break;
            case 2: refreshMoments(); midCards.show(midPanel, "MOMENTS"); showProfileRight(); break;
            case 3: refreshSearch(); midCards.show(midPanel, "SEARCH"); showProfileRight(); break;
        }
    }

    // ======== PROFILE MIDDLE ========
    private JPanel buildProfileMid() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(32, 24, 16, 24));

        pAvatar = new AvatarLabel(80, null);
        pAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        pAvatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pAvatar.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { chooseAvatar(); }
        });
        content.add(pAvatar);
        content.add(Box.createVerticalStrut(16));

        pName = new JLabel("", SwingConstants.CENTER);
        pName.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 20));
        pName.setForeground(TEXT_MAIN);
        pName.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(pName);
        content.add(Box.createVerticalStrut(4));

        pId = new JLabel("", SwingConstants.CENTER);
        pId.setFont(YH); pId.setForeground(TEXT_SUB);
        pId.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(pId);
        content.add(Box.createVerticalStrut(32));

        content.add(editRow("Name"));
        content.add(Box.createVerticalStrut(12));
        content.add(editRow("Workplace"));
        content.add(Box.createVerticalStrut(12));
        content.add(editRow("Hometown"));
        content.add(Box.createVerticalStrut(24));

        StyledButton saveBtn = new StyledButton("Save Changes", true);
        saveBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveBtn.addActionListener(e -> saveProfile());
        content.add(saveBtn);

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null); sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel editRow(String label) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(260, 40));
        JLabel l = new JLabel(label);
        l.setFont(YHB); l.setForeground(TEXT_SUB);
        l.setPreferredSize(new Dimension(80, 40));
        JTextField tf = new JTextField();
        tf.setFont(YH); tf.setForeground(TEXT_MAIN);
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(8, 8, 8, 8));
        row.add(l, BorderLayout.WEST);
        row.add(tf, BorderLayout.CENTER);
        // Store field reference based on label
        if ("Name".equals(label)) pNameF = tf;
        else if ("Workplace".equals(label)) pWorkF = tf;
        else if ("Hometown".equals(label)) pHomeF = tf;
        // Wrap with bottom separator
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(260, 42));
        wrap.add(row, BorderLayout.CENTER);
        JSeparator sep = new JSeparator();
        sep.setForeground(DIVIDER);
        wrap.add(sep, BorderLayout.SOUTH);
        return wrap;
    }

    // ======== PROFILE RIGHT ========
    private JPanel buildProfileRight() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(32, 32, 16, 32));

        rFriends = new JLabel("0");
        rFriends.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 24));
        rFriends.setForeground(BRAND);
        rPosts = new JLabel("0");
        rPosts.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 24));
        rPosts.setForeground(BRAND);
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 32, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.add(buildStatBox(rFriends, "Friends"));
        statsRow.add(buildStatBox(rPosts, "Posts"));
        p.add(statsRow);
        p.add(Box.createVerticalStrut(32));

        JLabel sigTitle = new JLabel("Signature");
        sigTitle.setFont(YHB); sigTitle.setForeground(TEXT_SUB);
        p.add(sigTitle);
        p.add(Box.createVerticalStrut(8));
        rSigArea = new JTextArea(3, 20);
        rSigArea.setFont(YH); rSigArea.setForeground(TEXT_MAIN);
        rSigArea.setLineWrap(true); rSigArea.setWrapStyleWord(true);
        rSigArea.setOpaque(false);
        rSigArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane sp = new JScrollPane(rSigArea);
        sp.setBorder(BorderFactory.createLineBorder(DIVIDER));
        sp.setOpaque(false); sp.getViewport().setOpaque(false);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(sp);
        p.add(Box.createVerticalStrut(16));

        StyledButton sigSave = new StyledButton("Save Signature", false);
        sigSave.setAlignmentX(Component.LEFT_ALIGNMENT);
        sigSave.addActionListener(e -> {
            User cur = network.getCurrentUser();
            if (cur != null) {
                cur.setSignature(rSigArea.getText().trim());
                LoginPanel.rewriteUsersFile(network);
                JOptionPane.showMessageDialog(this, "Signature saved!", "SnapTok", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        p.add(sigSave);

        p.add(Box.createVerticalGlue());
        StyledButton logoutBtn = new StyledButton("Logout", false);
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.addActionListener(e -> {
            network.setCurrentUser(null);
            mainGUI.showCard(MainGUI.LOGIN_CARD);
        });
        p.add(logoutBtn);

        return p;
    }

    // ======== FRIENDS MIDDLE ========
    private JPanel buildFriendsMid() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Search
        fSearch = new JTextField();
        fSearch.setFont(YH); fSearch.setForeground(TEXT_MAIN);
        fSearch.setBorder(new EmptyBorder(10, 12, 10, 12));
        fSearch.setBackground(INPUT_BG);
        fSearch.setCaretColor(BRAND);
        fSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { refreshFriendList(); }
        });
        JPanel searchWrap = new JPanel(new BorderLayout());
        searchWrap.setOpaque(false);
        searchWrap.add(fSearch, BorderLayout.CENTER);
        p.add(searchWrap, BorderLayout.NORTH);

        // Filter buttons
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filterRow.setOpaque(false);
        for (String f : new String[]{"All", "Same Hometown", "Same Workplace"}) {
            FilterChip chip = new FilterChip(f, f.equals(fFilter));
            chip.addActionListener(e -> { fFilter = f; refreshFriends(); });
            filterRow.add(chip);
        }
        searchWrap.add(filterRow, BorderLayout.SOUTH);

        // Friend list
        fModel = new DefaultListModel<>();
        fList = new JList<>(fModel);
        fList.setCellRenderer(new FriendCellRenderer());
        fList.setFixedCellHeight(60);
        fList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && fList.getSelectedValue() != null)
                showFriendDetail(fList.getSelectedValue());
        });
        JScrollPane sp = new JScrollPane(fList);
        sp.setBorder(null); sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ======== MOMENTS MIDDLE ========
    private JPanel buildMomentsMid() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Composer
        JPanel composer = new JPanel(new BorderLayout(8, 8));
        composer.setOpaque(false);
        JLabel compTitle = new JLabel("Share a moment");
        compTitle.setFont(YHB); compTitle.setForeground(TEXT_MAIN);
        composer.add(compTitle, BorderLayout.NORTH);

        mInput = new JTextArea(3, 20);
        mInput.setFont(YH); mInput.setForeground(TEXT_MAIN);
        mInput.setLineWrap(true); mInput.setWrapStyleWord(true);
        mInput.setBorder(new EmptyBorder(8, 8, 8, 8));
        mInput.setBackground(INPUT_BG);
        mInput.setCaretColor(BRAND);
        JScrollPane inputSp = new JScrollPane(mInput);
        inputSp.setBorder(null); inputSp.setOpaque(false);
        inputSp.getViewport().setOpaque(false);
        composer.add(inputSp, BorderLayout.CENTER);

        StyledButton postBtn = new StyledButton("Post", true);
        postBtn.addActionListener(e -> createPost());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        btnRow.setOpaque(false);
        btnRow.add(postBtn);
        composer.add(btnRow, BorderLayout.SOUTH);

        // Feed
        mFeed = new JPanel();
        mFeed.setLayout(new BoxLayout(mFeed, BoxLayout.Y_AXIS));
        mFeed.setOpaque(false);
        JScrollPane feedSp = new JScrollPane(mFeed);
        feedSp.setBorder(null); feedSp.setOpaque(false);
        feedSp.getViewport().setOpaque(false);
        feedSp.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, composer, feedSp);
        split.setDividerLocation(140);
        split.setResizeWeight(0);
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(4);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    // ======== SEARCH MIDDLE ========
    private JPanel buildSearchMid() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        sField = new JTextField();
        sField.setFont(YH); sField.setForeground(TEXT_MAIN);
        sField.setBorder(new EmptyBorder(10, 12, 10, 12));
        sField.setBackground(INPUT_BG);
        sField.setCaretColor(BRAND);
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.add(sField, BorderLayout.CENTER);
        StyledButton goBtn = new StyledButton("Go", false);
        goBtn.setPreferredSize(new Dimension(60, 40));
        goBtn.addActionListener(e -> performSearch());
        searchRow.add(goBtn, BorderLayout.EAST);
        p.add(searchRow, BorderLayout.NORTH);

        JLabel recLabel = new JLabel("Recommended for you");
        recLabel.setFont(YHB); recLabel.setForeground(TEXT_SUB);
        recLabel.setBorder(new EmptyBorder(16, 0, 8, 0));

        sModel = new DefaultListModel<>();
        sList = new JList<>(sModel);
        sList.setCellRenderer(new FriendCellRenderer());
        sList.setFixedCellHeight(60);
        sList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && sList.getSelectedValue() != null)
                showSearchUserDetail(sList.getSelectedValue());
        });
        JScrollPane sp = new JScrollPane(sList);
        sp.setBorder(null); sp.setOpaque(false);
        sp.getViewport().setOpaque(false);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);
        centerWrap.add(recLabel, BorderLayout.NORTH);
        centerWrap.add(sp, BorderLayout.CENTER);
        p.add(centerWrap, BorderLayout.CENTER);
        return p;
    }

    // ======== RIGHT DETAIL PANEL ========
    private JPanel buildDetailRight() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(32, 32, 16, 32));

        rAvatar = new AvatarLabel(80, null);
        rAvatar.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(rAvatar);
        p.add(Box.createVerticalStrut(16));

        rName = new JLabel("");
        rName.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 20));
        rName.setForeground(TEXT_MAIN);
        rName.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(rName);
        p.add(Box.createVerticalStrut(4));

        rId = new JLabel("");
        rId.setFont(YH); rId.setForeground(TEXT_SUB);
        rId.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(rId);
        p.add(Box.createVerticalStrut(24));

        rWork = detailRow("Workplace", "");
        p.add(rWork); p.add(Box.createVerticalStrut(8));
        rHome = detailRow("Hometown", "");
        p.add(rHome); p.add(Box.createVerticalStrut(8));
        rSig = detailRow("Signature", "");
        p.add(rSig); p.add(Box.createVerticalStrut(8));
        rMutual = detailRow("Mutual Friends", "0");
        p.add(rMutual);
        p.add(Box.createVerticalStrut(24));

        rActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rActionPanel.setOpaque(false);
        rActionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(rActionPanel);

        p.add(Box.createVerticalStrut(24));
        rExtraPanel = new JPanel(new BorderLayout());
        rExtraPanel.setOpaque(false);
        rExtraPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(rExtraPanel);

        JScrollPane sp = new JScrollPane(p);
        sp.setBorder(null); sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel detailRow(String label, String val) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(400, 28));
        JLabel l = new JLabel(label + ": ");
        l.setFont(YHB); l.setForeground(TEXT_SUB);
        JLabel v = new JLabel(val);
        v.setFont(YH); v.setForeground(TEXT_MAIN);
        v.setName(label);
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildStatBox(JLabel num, String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        num.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel t = new JLabel(title);
        t.setFont(YH); t.setForeground(TEXT_SUB);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(num); p.add(t);
        return p;
    }

    // ======== REFRESH METHODS ========
    void refreshProfile() {
        User u = network.getCurrentUser();
        if (u == null) return;
        pAvatar.repaint();
        pName.setText(u.getName());
        pId.setText("@" + u.getUserId());
        pNameF.setText(u.getName());
        pWorkF.setText(u.getWorkplace());
        pHomeF.setText(u.getHometown());
        // Update right stats
        rFriends.setText(String.valueOf(u.getFriends().size()));
        rPosts.setText(String.valueOf(u.getPosts().size()));
        rSigArea.setText(u.getSignature());
    }

    void refreshFriends() {
        refreshFriendList();
    }

    private void refreshFriendList() {
        fModel.clear();
        User cur = network.getCurrentUser();
        if (cur == null) return;
        List<User> friends = cur.getFriends();
        String query = fSearch != null ? fSearch.getText().trim().toLowerCase() : "";

        for (User f : friends) {
            if (!query.isEmpty() && !f.getName().toLowerCase().contains(query)
                    && !f.getUserId().toLowerCase().contains(query)) continue;
            if ("Same Hometown".equals(fFilter) && !f.getHometown().equalsIgnoreCase(cur.getHometown())) continue;
            if ("Same Workplace".equals(fFilter) && !f.getWorkplace().equalsIgnoreCase(cur.getWorkplace())) continue;
            fModel.addElement(f);
        }
    }

    void refreshMoments() {
        mFeed.removeAll();
        List<Post> posts = network.getAllPosts();
        User cur = network.getCurrentUser();
        if (posts.isEmpty()) {
            JLabel empty = new JLabel("No moments yet. Share something!");
            empty.setFont(YH); empty.setForeground(TEXT_HINT);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.setBorder(new EmptyBorder(32, 0, 0, 0));
            mFeed.add(empty);
        } else {
            for (Post post : posts) {
                mFeed.add(createPostCard(post, cur));
                mFeed.add(Box.createVerticalStrut(8));
            }
        }
        mFeed.revalidate(); mFeed.repaint();
    }

    void refreshSearch() {
        sModel.clear();
        User cur = network.getCurrentUser();
        if (cur == null) return;
        Map<User, String> recs = network.getFriendRecommendations(cur);
        for (User u : recs.keySet()) sModel.addElement(u);
    }

    // ======== ACTIONS ========
    private void saveProfile() {
        User cur = network.getCurrentUser();
        if (cur == null) return;
        String name = pNameF.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Name cannot be empty.", "SnapTok", JOptionPane.WARNING_MESSAGE); return; }
        cur.setName(name);
        cur.setWorkplace(pWorkF.getText().trim().isEmpty() ? "Unknown" : pWorkF.getText().trim());
        cur.setHometown(pHomeF.getText().trim().isEmpty() ? "Unknown" : pHomeF.getText().trim());
        refreshProfile();
        LoginPanel.rewriteUsersFile(network);
        JOptionPane.showMessageDialog(this, "Profile updated!", "SnapTok", JOptionPane.INFORMATION_MESSAGE);
    }

    private void chooseAvatar() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            User cur = network.getCurrentUser();
            if (cur != null) {
                cur.setAvatarPath(fc.getSelectedFile().getAbsolutePath());
                LoginPanel.rewriteUsersFile(network);
                refreshProfile();
            }
        }
    }

    private void createPost() {
        User cur = network.getCurrentUser();
        if (cur == null) return;
        String text = mInput.getText().trim();
        if (text.isEmpty()) { JOptionPane.showMessageDialog(this, "Write something first.", "SnapTok", JOptionPane.WARNING_MESSAGE); return; }
        network.createPost(cur, text);
        mInput.setText("");
        refreshMoments();
    }

    private void toggleLike(Post post) {
        User cur = network.getCurrentUser();
        if (cur == null) return;
        if (post.isLikedBy(cur)) post.removeLike(cur); else post.addLike(cur);
        refreshMoments();
    }

    private void performSearch() {
        sModel.clear();
        String q = sField.getText().trim().toLowerCase();
        if (q.isEmpty()) { refreshSearch(); return; }
        User cur = network.getCurrentUser();
        for (User u : network.getAllUsers()) {
            if (u.equals(cur)) continue;
            if (u.getName().toLowerCase().contains(q) || u.getUserId().toLowerCase().contains(q)
                    || u.getWorkplace().toLowerCase().contains(q) || u.getHometown().toLowerCase().contains(q)) {
                sModel.addElement(u);
            }
        }
    }

    // ======== SHOW DETAIL ========
    void showFriendDetail(User friend) {
        detailUser = friend;
        updateDetailPanel(friend, true);
        rightCards.show(rightPanel, "DETAIL");
    }

    void showSearchUserDetail(User user) {
        detailUser = user;
        updateDetailPanel(user, false);
        rightCards.show(rightPanel, "DETAIL");
    }

    void showPostDetail(Post post) {
        detailPost = post;
        // Update detail panel for post
        updatePostDetailPanel(post);
        rightCards.show(rightPanel, "DETAIL");
    }

    void showProfileRight() {
        rightCards.show(rightPanel, "PROFILE");
        User cur = network.getCurrentUser();
        if (cur != null) {
            rFriends.setText(String.valueOf(cur.getFriends().size()));
            rPosts.setText(String.valueOf(cur.getPosts().size()));
            rSigArea.setText(cur.getSignature());
        }
    }

    private void updateDetailPanel(User user, boolean isFriend) {
        User cur = network.getCurrentUser();
        ((AvatarLabel) rAvatar).user = user;
        rAvatar.repaint();
        rName.setText(user.getName());
        rId.setText("@" + user.getUserId());
        setDetailValue(rWork, user.getWorkplace());
        setDetailValue(rHome, user.getHometown());
        setDetailValue(rSig, user.getSignature().isEmpty() ? "No signature" : user.getSignature());
        List<User> mutual = cur != null ? cur.getMutualFriends(user) : new ArrayList<>();
        setDetailValue(rMutual, mutual.size() + " person(s)");

        rActionPanel.removeAll();
        if (isFriend) {
            StyledButton viewFriends = new StyledButton("View Their Friends", false);
            viewFriends.addActionListener(e -> showFriendsOfFriend(user));
            rActionPanel.add(viewFriends);
            StyledButton removeBtn = new StyledButton("Remove Friend", false);
            removeBtn.addActionListener(e -> {
                if (cur != null && JOptionPane.showConfirmDialog(this, "Remove " + user.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    cur.removeFriend(user);
                    refreshFriends();
                    showProfileRight();
                }
            });
            rActionPanel.add(removeBtn);
        } else {
            if (cur != null && !cur.isFriendWith(user) && !user.equals(cur)) {
                StyledButton addBtn = new StyledButton("Add Friend", true);
                addBtn.addActionListener(e -> {
                    cur.addFriend(user);
                    JOptionPane.showMessageDialog(this, user.getName() + " added!", "SnapTok", JOptionPane.INFORMATION_MESSAGE);
                    refreshSearch();
                });
                rActionPanel.add(addBtn);
            }
        }
        rExtraPanel.removeAll();
        rExtraPanel.revalidate(); rExtraPanel.repaint();
        rightCards.show(rightPanel, "DETAIL");
    }

    private void updatePostDetailPanel(Post post) {
        ((AvatarLabel) rAvatar).user = post.getAuthor();
        rAvatar.repaint();
        rName.setText(post.getAuthor().getName());
        rId.setText(post.getTimestampString());
        setDetailValue(rWork, "");
        setDetailValue(rHome, "");
        setDetailValue(rSig, post.getContent());
        setDetailValue(rMutual, post.getLikeCount() + " like(s)");

        rActionPanel.removeAll();
        User cur = network.getCurrentUser();
        if (cur != null) {
            boolean liked = post.isLikedBy(cur);
            StyledButton likeBtn = new StyledButton(liked ? "Unlike" : "Like", liked);
            likeBtn.addActionListener(e -> { toggleLike(post); showPostDetail(post); });
            rActionPanel.add(likeBtn);
        }
        rExtraPanel.removeAll();
        if (post.getLikeCount() > 0) {
            JLabel likers = new JLabel("<html>Liked by: " + post.getLikerIdsString().replace(",", ", ") + "</html>");
            likers.setFont(YH); likers.setForeground(TEXT_SUB);
            rExtraPanel.add(likers, BorderLayout.NORTH);
        }
        rExtraPanel.revalidate(); rExtraPanel.repaint();
    }

    private void showFriendsOfFriend(User user) {
        User cur = network.getCurrentUser();
        rExtraPanel.removeAll();
        JLabel title = new JLabel(user.getName() + "'s Friends");
        title.setFont(YHB); title.setForeground(TEXT_MAIN);
        rExtraPanel.add(title, BorderLayout.NORTH);

        DefaultListModel<User> model = new DefaultListModel<>();
        for (User f : user.getFriends()) model.addElement(f);
        JList<User> list = new JList<>(model);
        list.setCellRenderer(new FriendCellRenderer());
        list.setFixedCellHeight(50);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null)
                showSearchUserDetail(list.getSelectedValue());
        });
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null); sp.setOpaque(false); sp.getViewport().setOpaque(false);
        sp.setPreferredSize(new Dimension(250, 200));
        rExtraPanel.add(sp, BorderLayout.CENTER);
        rExtraPanel.revalidate(); rExtraPanel.repaint();
    }

    private void setDetailValue(JPanel row, String val) {
        if (row.getComponentCount() >= 2) {
            ((JLabel) row.getComponent(1)).setText(val);
        }
    }

    // ======== POST CARD ========
    private JPanel createPostCard(Post post, User cur) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));
        card.setMaximumSize(new Dimension(280, 120));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel author = new JLabel(post.getAuthor().getName());
        author.setFont(YHB); author.setForeground(BRAND);
        top.add(author, BorderLayout.WEST);
        JLabel time = new JLabel(post.getTimestampString());
        time.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 11));
        time.setForeground(TEXT_HINT);
        top.add(time, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JLabel content = new JLabel("<html><body style='width:240px'>" + post.getContent() + "</body></html>");
        content.setFont(YH); content.setForeground(TEXT_MAIN);
        card.add(content, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottom.setOpaque(false);
        boolean liked = cur != null && post.isLikedBy(cur);
        JLabel likeLabel = new JLabel((liked ? "\u2665 " : "\u2661 ") + post.getLikeCount());
        likeLabel.setFont(YH);
        likeLabel.setForeground(liked ? new Color(239, 68, 68) : TEXT_HINT);
        likeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        likeLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { toggleLike(post); }
        });
        bottom.add(likeLabel);
        JLabel viewLabel = new JLabel("View \u203A");
        viewLabel.setFont(YH); viewLabel.setForeground(BRAND);
        viewLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { showPostDetail(post); }
        });
        bottom.add(viewLabel);
        card.add(bottom, BorderLayout.SOUTH);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBackground(HOVER_BG); }
            public void mouseExited(MouseEvent e) { card.setBackground(Color.WHITE); }
            public void mouseClicked(MouseEvent e) { showPostDetail(post); }
        });
        return card;
    }

    // ======== HELPERS ========
    static Image loadAvatarImage(String path, int size) {
        if (path == null || path.isEmpty()) return null;
        try {
            Image img = ImageIO.read(new File(path));
            return img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        } catch (Exception e) { return null; }
    }

    // ======== INNER CLASSES ========

    /** Thin vertical separator */
    static class JSep extends JPanel {
        JSep() { setPreferredSize(new Dimension(1, 0)); }
        @Override
        protected void paintComponent(Graphics g) { g.setColor(DIVIDER); g.fillRect(0, 0, 1, getHeight()); }
    }

    /** Circular avatar label */
    static class AvatarLabel extends JLabel {
        int size; User user;
        AvatarLabel(int size, User user) {
            this.size = size; this.user = user;
            setPreferredSize(new Dimension(size, size));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape circle = new Ellipse2D.Float(0, 0, size, size);
            g2.setClip(circle);
            if (user != null && !user.getAvatarPath().isEmpty()) {
                Image img = loadAvatarImage(user.getAvatarPath(), size);
                if (img != null) { g2.drawImage(img, 0, 0, null); g2.dispose(); return; }
            }
            g2.setColor(new Color(209, 213, 219));
            g2.fill(circle);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, size / 2));
            String init = (user != null && !user.getName().isEmpty()) ? user.getName().substring(0, 1).toUpperCase() : "?";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(init, (size - fm.stringWidth(init)) / 2, (size + fm.getAscent()) / 2 - 4);
            g2.dispose();
        }
    }

    /** Nav icon button */
    class NavIcon extends JPanel {
        int idx; String tip; boolean hover;
        NavIcon(int idx, String tip) {
            this.idx = idx; this.tip = tip;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(tip);
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseClicked(MouseEvent e) {
                    if (idx <= 3) selectNav(idx);
                    else if (idx == 10) saveNetworkFile();
                    else if (idx == 11) loadNetworkFile();
                    else if (idx == 12) { network.setCurrentUser(null); mainGUI.showCard(MainGUI.LOGIN_CARD); }
                }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (hover && idx <= 3) { g2.setColor(HOVER_BG); g2.fillRect(0, 4, 60, 40); }
            boolean sel = (idx == navIdx && idx <= 3);
            Color c = sel ? BRAND : new Color(75, 85, 99);
            g2.setColor(c);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = 30, cy = 24;
            if (idx == 0) drawPersonIcon(g2, cx, cy);
            else if (idx == 1) drawFriendsIcon(g2, cx, cy);
            else if (idx == 2) drawMomentsIcon(g2, cx, cy);
            else if (idx == 3) drawSearchIcon(g2, cx, cy);
            else if (idx == 10) drawSaveIcon(g2, cx, cy);
            else if (idx == 11) drawLoadIcon(g2, cx, cy);
            else if (idx == 12) drawLogoutIcon(g2, cx, cy);
            if (sel) { g2.setColor(BRAND); g2.fillRect(0, 8, 3, 32); }
            g2.dispose();
        }
    }

    private void drawPersonIcon(Graphics2D g, int cx, int cy) {
        g.drawOval(cx - 5, cy - 9, 10, 10);
        g.drawArc(cx - 10, cy + 1, 20, 16, 0, 180);
    }
    private void drawFriendsIcon(Graphics2D g, int cx, int cy) {
        g.drawOval(cx - 8, cy - 8, 8, 8);
        g.drawOval(cx, cy - 8, 8, 8);
        g.drawArc(cx - 12, cy + 2, 14, 12, 0, 180);
        g.drawArc(cx - 2, cy + 2, 14, 12, 0, 180);
    }
    private void drawMomentsIcon(Graphics2D g, int cx, int cy) {
        g.drawRoundRect(cx - 10, cy - 8, 20, 14, 6, 6);
        g.drawLine(cx - 4, cy + 6, cx - 6, cy + 10);
        g.drawLine(cx - 4, cy + 6, cx, cy + 6);
    }
    private void drawSearchIcon(Graphics2D g, int cx, int cy) {
        g.drawOval(cx - 7, cy - 7, 12, 12);
        g.drawLine(cx + 2, cy + 2, cx + 8, cy + 8);
    }
    private void drawSaveIcon(Graphics2D g, int cx, int cy) {
        g.drawRect(cx - 8, cy - 8, 16, 16);
        g.drawLine(cx - 4, cy - 8, cx - 4, cy - 2);
        g.drawLine(cx + 4, cy - 8, cx + 4, cy - 2);
        g.drawRect(cx - 5, cy + 1, 10, 7);
    }
    private void drawLoadIcon(Graphics2D g, int cx, int cy) {
        g.drawRoundRect(cx - 9, cy - 4, 18, 12, 4, 4);
        g.drawLine(cx, cy - 8, cx, cy + 2);
        g.drawLine(cx - 4, cy - 4, cx, cy - 8);
        g.drawLine(cx + 4, cy - 4, cx, cy - 8);
    }
    private void drawLogoutIcon(Graphics2D g, int cx, int cy) {
        g.drawArc(cx - 8, cy - 8, 16, 16, 90, 270);
        g.drawLine(cx, cy, cx + 10, cy);
        g.drawLine(cx + 6, cy - 4, cx + 10, cy);
        g.drawLine(cx + 6, cy + 4, cx + 10, cy);
    }

    /** Styled button */
    static class StyledButton extends JButton {
        private boolean primary, hover;
        StyledButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(YHB); setForeground(primary ? Color.WHITE : BRAND);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(primary ? 220 : 140, primary ? 44 : 36));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int yOff = hover ? -2 : 0;
            if (primary) {
                if (hover) { g2.setColor(new Color(59, 130, 246, 30)); g2.fillRoundRect(2, yOff + 3, getWidth() - 4, getHeight() - 2, 12, 12); }
                g2.setColor(hover ? BRAND_DARK : BRAND);
                g2.fillRoundRect(0, yOff, getWidth(), getHeight(), 12, 12);
            } else {
                if (hover) { g2.setColor(new Color(59, 130, 246, 15)); g2.fillRoundRect(0, yOff, getWidth(), getHeight(), 12, 12); }
                g2.setColor(BRAND); g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, yOff + 1, getWidth() - 2, getHeight() - 2, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Filter chip button */
    class FilterChip extends JButton {
        private boolean active, hover;
        FilterChip(String text, boolean active) {
            super(text);
            this.active = active;
            setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 12));
            setForeground(active ? Color.WHITE : TEXT_SUB);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(4, 10, 4, 10));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) g2.setColor(BRAND);
            else if (hover) g2.setColor(HOVER_BG);
            else g2.setColor(INPUT_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Friend list cell renderer */
    class FriendCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            User u = (User) value;
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(true);
            row.setBackground(isSelected ? new Color(239, 246, 255) : Color.WHITE);
            row.setBorder(new EmptyBorder(8, 12, 8, 12));

            AvatarLabel av = new AvatarLabel(40, u);
            row.add(av, BorderLayout.WEST);

            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setOpaque(false);
            JLabel name = new JLabel(u.getName());
            name.setFont(YHB); name.setForeground(TEXT_MAIN);
            info.add(name);
            String latest = "";
            if (!u.getPosts().isEmpty()) {
                Post last = u.getPosts().get(u.getPosts().size() - 1);
                latest = last.getContent();
                if (latest.length() > 20) latest = latest.substring(0, 20) + "...";
            }
            JLabel preview = new JLabel(latest.isEmpty() ? u.getWorkplace() : latest);
            preview.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 12));
            preview.setForeground(TEXT_HINT);
            info.add(preview);
            row.add(info, BorderLayout.CENTER);

            row.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { if (!isSelected) row.setBackground(HOVER_BG); }
                public void mouseExited(MouseEvent e) { if (!isSelected) row.setBackground(Color.WHITE); }
            });
            return row;
        }
    }

    /** File operations */
    static void loadNetworkInto(SocialNetwork network, java.io.File file) throws Exception {
        SocialNetwork ld = FileManager.loadNetwork(file.getAbsolutePath());
        for (User u : ld.getAllUsers()) network.addUser(u);
        network.setPostCounter(ld.getPostCounter());
        LoginPanel.rewriteUsersFile(network);
    }

    private void saveNetworkFile() {
        JFileChooser c = new JFileChooser();
        if (c.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                FileManager.saveNetwork(c.getSelectedFile().getAbsolutePath(), network);
                JOptionPane.showMessageDialog(this, "Network saved!", "SnapTok", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void loadNetworkFile() {
        JFileChooser c = new JFileChooser();
        if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                loadNetworkInto(network, c.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Network loaded!", "SnapTok", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
