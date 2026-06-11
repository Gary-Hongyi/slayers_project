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
 * Apple-style three-column main content panel.
 * Left: 64px black nav | Middle: 300px list | Right: detail view.
 */
public class MainContentPanel extends JPanel {

    private MainGUI mainGUI;
    private SocialNetwork network;
    // Apple Design System — clean, minimal, photography-forward
    static final String FONT_FAMILY = "Segoe UI";
    static final Font YH = new Font(FONT_FAMILY, Font.PLAIN, 14);       // caption 14/400
    static final Font YHB = new Font(FONT_FAMILY, Font.BOLD, 14);       // caption-strong 14/600
    static final Font YH_SM = new Font(FONT_FAMILY, Font.PLAIN, 12);    // fine-print 12/400
    static final Font YH_XS = new Font(FONT_FAMILY, Font.PLAIN, 11);    // micro 11/400
    static final Font YH_LG = new Font(FONT_FAMILY, Font.BOLD, 17);     // body-strong 17/600
    static final Font YH_XL = new Font(FONT_FAMILY, Font.BOLD, 21);     // tagline 21/600
    static final Font YH_XXL = new Font(FONT_FAMILY, Font.BOLD, 28);    // lead 28/400
    static final Color BRAND = new Color(0, 102, 204);         // #0066cc Action Blue
    static final Color BRAND_DARK = new Color(0, 89, 178);     // press state
    static final Color TEXT_MAIN = new Color(29, 29, 31);      // #1d1d1f ink
    static final Color TEXT_SUB = new Color(122, 122, 122);    // #7a7a7a muted-48
    static final Color TEXT_HINT = new Color(122, 122, 122);   // #7a7a7a muted-48
    static final Color DIVIDER = new Color(240, 240, 240);     // #f0f0f0 divider-soft
    static final Color HAIRLINE = new Color(224, 224, 224);    // #e0e0e0 hairline
    static final Color HOVER_BG = new Color(245, 245, 247);    // #f5f5f7 parchment
    static final Color CANVAS = Color.WHITE;                    // canvas #ffffff
    static final Color SURFACE_BLACK = new Color(0, 0, 0);     // global nav

    /** Resolves relative avatar paths against the working directory. */
    static String resolveAvatarPath(String path) {
        if (path == null || path.isEmpty()) return null;
        File f = new File(path);
        if (f.isAbsolute()) return f.exists() ? path : null;
        File resolved = new File(LoginPanel.getProjectRoot(), path);
        return resolved.exists() ? resolved.getAbsolutePath() : null;
    }

    private int navIdx = 0;
    private CardLayout midCards, rightCards;
    private JPanel midPanel, rightPanel;
    private User detailUser = null;
    private Post detailPost = null;

    // Profile
    private JLabel pAvatar, pName, pId;
    private JTextField pNameF, pWorkF, pHomeF;
    private JTextArea pSigArea;
    private JPanel profilePostFeed;
    // Friends
    private DefaultListModel<User> fModel;
    private JList<User> fList;
    private JTextField fSearch;
    private String fFilter = "All";
    private final List<FilterChip> fFilterChips = new ArrayList<>();
    // Moments
    private JTextArea mInput;
    private JPanel mFeed;
    // Search
    private JTextField sField;
    private DefaultListModel<User> sModel;
    private JList<User> sList;
    private String sFilter = "Mutual Friends";
    private final List<FilterChip> sFilterChips = new ArrayList<>();
    private final Map<User, String> sRecommendationReasons = new HashMap<>();
    private DefaultListModel<User> searchResultModel;
    private JList<User> searchResultList;
    // Right detail labels
    private JLabel rAvatar, rName, rId;
    private JPanel rRemark, rWork, rHome, rSig, rMutual;
    private JLabel rFriends, rPosts;
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
        rightPanel.add(buildEmptyRight(), "EMPTY");
        rightPanel.add(buildSearchRight(), "SEARCH");
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

    // ======== LEFT NAV — Apple global nav style ========
    private JPanel buildNav() {
        JPanel nav = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(SURFACE_BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        nav.setPreferredSize(new Dimension(64, 0));
        String[] tips = {"Profile", "Friends", "Moments", "Search"};
        for (int i = 0; i < 4; i++) {
            NavIcon ni = new NavIcon(i, tips[i]);
            ni.setBounds(0, 28 + i * 60, 64, 52);
            nav.add(ni);
        }
        NavActionIcon save = new NavActionIcon(0, "Save");
        NavActionIcon load = new NavActionIcon(1, "Load");
        NavActionIcon logout = new NavActionIcon(2, "Logout");
        save.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { saveNetworkFile(); }
        });
        load.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { loadNetworkFile(); }
        });
        logout.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                mainGUI.saveNetworkNow();
                network.setCurrentUser(null);
                mainGUI.showCard(MainGUI.LOGIN_CARD);
            }
        });
        save.setBounds(0, 420, 64, 52);
        load.setBounds(0, 480, 64, 52);
        logout.setBounds(0, 540, 64, 52);
        nav.add(save);
        nav.add(load);
        nav.add(logout);
        nav.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int y = Math.max(280, nav.getHeight() - 180);
                save.setBounds(0, y, 64, 52);
                load.setBounds(0, y + 60, 64, 52);
                logout.setBounds(0, y + 120, 64, 52);
            }
        });
        return nav;
    }

    private void selectNav(int idx) {
        navIdx = idx;
        repaint();
        switch (idx) {
            case 0: refreshProfile(); midCards.show(midPanel, "PROFILE"); rightCards.show(rightPanel, "PROFILE"); break;
            case 1: refreshFriends(); midCards.show(midPanel, "FRIENDS"); showEmptyRight(); break;
            case 2: refreshMoments(); midCards.show(midPanel, "MOMENTS"); showProfileRight(); break;
            case 3: refreshSearch(); midCards.show(midPanel, "SEARCH"); showSearchRight(); break;
        }
    }

    // ======== PROFILE MIDDLE ========
    private JPanel buildProfileMid() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(24, 24, 16, 24));

        pAvatar = new AvatarLabel(80, null);
        pAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        pAvatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pAvatar.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { chooseAvatar(); }
        });
        content.add(pAvatar);
        content.add(Box.createVerticalStrut(16));

        pName = new JLabel("", SwingConstants.CENTER);
        pName.setFont(YH_XXL);
        pName.setForeground(TEXT_MAIN);
        pName.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(pName);
        content.add(Box.createVerticalStrut(4));

        pId = new JLabel("", SwingConstants.CENTER);
        pId.setFont(YH); pId.setForeground(TEXT_SUB);
        pId.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(pId);
        content.add(Box.createVerticalStrut(20));

        content.add(profileTextArea("Signature"));
        content.add(Box.createVerticalStrut(12));
        content.add(editRow("Name"));
        content.add(Box.createVerticalStrut(12));
        content.add(editRow("Workplace"));
        content.add(Box.createVerticalStrut(12));
        content.add(editRow("Hometown"));
        content.add(Box.createVerticalStrut(20));

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

    private JPanel profileTextArea(String label) {
        JPanel wrap = new JPanel(new BorderLayout(0, 6));
        wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(260, 116));
        JLabel l = new JLabel(label);
        l.setFont(YHB); l.setForeground(TEXT_SUB);
        wrap.add(l, BorderLayout.NORTH);

        pSigArea = new JTextArea(3, 20);
        pSigArea.setFont(YH); pSigArea.setForeground(TEXT_MAIN);
        pSigArea.setLineWrap(true); pSigArea.setWrapStyleWord(true);
        pSigArea.setOpaque(false);
        pSigArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane sp = new JScrollPane(pSigArea);
        sp.setBorder(BorderFactory.createLineBorder(HAIRLINE));
        sp.setOpaque(false); sp.getViewport().setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
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
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(32, 32, 24, 32));

        rFriends = new JLabel("0");
        rFriends.setFont(YH_XXL);
        rFriends.setForeground(BRAND);
        rPosts = new JLabel("0");
        rPosts.setFont(YH_XXL);
        rPosts.setForeground(BRAND);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("My Posts");
        title.setFont(YH_XL);
        title.setForeground(TEXT_MAIN);
        top.add(title, BorderLayout.WEST);

        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 28, 0));
        statsRow.setOpaque(false);
        statsRow.add(buildStatBox(rFriends, "Friends"));
        statsRow.add(buildStatBox(rPosts, "Posts"));
        top.add(statsRow, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        profilePostFeed = new JPanel();
        profilePostFeed.setLayout(new BoxLayout(profilePostFeed, BoxLayout.Y_AXIS));
        profilePostFeed.setOpaque(false);
        JScrollPane postScroll = new JScrollPane(profilePostFeed);
        postScroll.setBorder(null);
        postScroll.setOpaque(false);
        postScroll.getViewport().setOpaque(false);
        postScroll.getVerticalScrollBar().setUnitIncrement(16);
        postScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(postScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(16, 0, 0, 0));
        StyledButton logoutBtn = new StyledButton("Logout", false);
        logoutBtn.addActionListener(e -> {
            network.setCurrentUser(null);
            mainGUI.showCard(MainGUI.LOGIN_CARD);
        });
        bottom.add(logoutBtn);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    // ======== FRIENDS MIDDLE ========
    private JPanel buildFriendsMid() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Search — white bg + hairline border for visibility
        fSearch = new PlaceholderField("Search by name...");
        fSearch.setFont(YH); fSearch.setForeground(TEXT_MAIN);
        fSearch.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(HAIRLINE, 1),
                new EmptyBorder(9, 12, 9, 12)));
        fSearch.setBackground(Color.WHITE);
        fSearch.setCaretColor(BRAND);
        fSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                refreshFriendList();
                showEmptyRight();
            }
        });
        JPanel searchWrap = new JPanel(new BorderLayout());
        searchWrap.setOpaque(false);
        searchWrap.add(fSearch, BorderLayout.CENTER);
        p.add(searchWrap, BorderLayout.NORTH);

        // Filter buttons
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filterRow.setOpaque(false);
        fFilterChips.clear();
        for (String f : new String[]{"All", "Same Hometown", "Same Workplace"}) {
            FilterChip chip = new FilterChip(f, f.equals(fFilter));
            fFilterChips.add(chip);
            chip.addActionListener(e -> {
                fFilter = f;
                updateFriendFilterChips();
                refreshFriends();
                if (fList != null) fList.clearSelection();
                showEmptyRight();
            });
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

        // Top bar with title and "New Post" button
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JLabel title = new JLabel("Moments");
        title.setFont(YHB); title.setForeground(TEXT_MAIN);
        topBar.add(title, BorderLayout.WEST);
        StyledButton newPostBtn = new StyledButton("New Post", true);
        newPostBtn.setPreferredSize(new Dimension(120, 36));
        newPostBtn.addActionListener(e -> showPostModal());
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(newPostBtn);
        topBar.add(btnWrap, BorderLayout.EAST);
        p.add(topBar, BorderLayout.NORTH);

        // Feed
        mFeed = new JPanel();
        mFeed.setLayout(new BoxLayout(mFeed, BoxLayout.Y_AXIS));
        mFeed.setOpaque(false);
        JScrollPane feedSp = new JScrollPane(mFeed);
        feedSp.setBorder(null); feedSp.setOpaque(false);
        feedSp.getViewport().setOpaque(false);
        feedSp.getVerticalScrollBar().setUnitIncrement(16);
        feedSp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(feedSp, BorderLayout.CENTER);
        return p;
    }

    /** Opens a modal dialog for creating a new post */
    private void showPostModal() {
        User cur = network.getCurrentUser();
        if (cur == null) return;
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "New Post", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(HAIRLINE, 1),
                new EmptyBorder(24, 24, 20, 24)));

        JLabel title = new JLabel("Share a moment");
        title.setFont(YH_LG);
        title.setForeground(TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(14));

        JTextArea input = new JTextArea(4, 30);
        input.setFont(YH); input.setForeground(TEXT_MAIN);
        input.setLineWrap(true); input.setWrapStyleWord(true);
        input.setBorder(new EmptyBorder(10, 10, 10, 10));
        input.setBackground(Color.WHITE);
        input.setCaretColor(BRAND);
        JScrollPane sp = new JScrollPane(input);
        sp.setBorder(BorderFactory.createLineBorder(HAIRLINE));
        sp.setOpaque(false); sp.getViewport().setOpaque(false);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setMaximumSize(new Dimension(420, 120));
        panel.add(sp);
        panel.add(Box.createVerticalStrut(16));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        StyledButton cancel = new StyledButton("Cancel", false);
        cancel.addActionListener(e -> dialog.dispose());
        StyledButton post = new StyledButton("Post", true);
        post.addActionListener(e -> {
            String text = input.getText().trim();
            if (text.isEmpty()) { JOptionPane.showMessageDialog(dialog, "Write something first.", "SnapTok", JOptionPane.WARNING_MESSAGE); return; }
            network.createPost(cur, text);
            mainGUI.saveNetworkNow();
            dialog.dispose();
            refreshMoments();
            refreshProfile();
        });
        btns.add(cancel); btns.add(post);
        panel.add(btns);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton(post);
        SwingUtilities.invokeLater(input::requestFocusInWindow);
        dialog.setVisible(true);
    }

    // ======== SEARCH MIDDLE ========
    private JPanel buildSearchMid() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        JLabel recLabel = new JLabel("Recommended for you");
        recLabel.setFont(YHB); recLabel.setForeground(TEXT_MAIN);
        recLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(recLabel);
        top.add(Box.createVerticalStrut(10));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        filterRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        sFilterChips.clear();
        for (String filter : new String[]{"Mutual Friends", "Same Workplace", "Same Hometown"}) {
            FilterChip chip = new FilterChip(filter, filter.equals(sFilter));
            sFilterChips.add(chip);
            chip.addActionListener(e -> {
                sFilter = filter;
                updateSearchFilterChips();
                refreshSearch();
                if (sList != null) sList.clearSelection();
            });
            filterRow.add(chip);
        }
        top.add(filterRow);
        top.add(Box.createVerticalStrut(12));
        p.add(top, BorderLayout.NORTH);

        sModel = new DefaultListModel<>();
        sList = new JList<>(sModel);
        sList.setCellRenderer(new RecommendationCellRenderer());
        sList.setFixedCellHeight(68);
        sList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && sList.getSelectedValue() != null)
                showSearchUserDetail(sList.getSelectedValue());
        });
        JScrollPane sp = new JScrollPane(sList);
        sp.setBorder(null); sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSearchRight() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(32, 32, 24, 32));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        JLabel title = new JLabel("Search Users");
        title.setFont(YH_XL);
        title.setForeground(TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(title);
        top.add(Box.createVerticalStrut(12));

        sField = new PlaceholderField("Search by name or ID...");
        sField.setFont(YH); sField.setForeground(TEXT_MAIN);
        sField.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(HAIRLINE, 1),
                new EmptyBorder(9, 12, 9, 12)));
        sField.setBackground(Color.WHITE);
        sField.setCaretColor(BRAND);
        sField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { performSearch(); }
        });

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        searchRow.add(sField, BorderLayout.CENTER);
        StyledButton goBtn = new StyledButton("Go", false);
        goBtn.setPreferredSize(new Dimension(70, 40));
        goBtn.addActionListener(e -> performSearch());
        searchRow.add(goBtn, BorderLayout.EAST);
        top.add(searchRow);
        top.add(Box.createVerticalStrut(18));
        p.add(top, BorderLayout.NORTH);

        searchResultModel = new DefaultListModel<>();
        searchResultList = new JList<>(searchResultModel);
        searchResultList.setCellRenderer(new FriendCellRenderer());
        searchResultList.setFixedCellHeight(64);
        searchResultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && searchResultList.getSelectedValue() != null)
                showSearchUserDetail(searchResultList.getSelectedValue());
        });
        JScrollPane sp = new JScrollPane(searchResultList);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildEmptyRight() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        EmptyChatIcon icon = new EmptyChatIcon();
        icon.setPreferredSize(new Dimension(96, 72));
        p.add(icon);
        return p;
    }

    // ======== RIGHT DETAIL PANEL ========
    private JPanel buildDetailRight() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        // Back button bar
        JPanel backBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backBar.setOpaque(false);
        backBar.setBorder(new EmptyBorder(12, 16, 0, 0));
        StyledButton backBtn = new StyledButton("Back", false);
        backBtn.setPreferredSize(new Dimension(92, 34));
        backBtn.setToolTipText("Go back");
        backBtn.addActionListener(e -> navigateBack());
        backBar.add(backBtn);
        p.add(backBar, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 32, 32, 32));
        content.setMaximumSize(new Dimension(430, Integer.MAX_VALUE));

        JPanel head = new JPanel(new BorderLayout(18, 0));
        head.setOpaque(false);
        head.setMaximumSize(new Dimension(430, 88));

        rAvatar = new AvatarLabel(72, null);
        head.add(rAvatar, BorderLayout.WEST);

        JPanel headText = new JPanel();
        headText.setLayout(new BoxLayout(headText, BoxLayout.Y_AXIS));
        headText.setOpaque(false);

        rName = new JLabel("");
        rName.setFont(YH_LG);
        rName.setForeground(TEXT_MAIN);
        headText.add(rName);
        headText.add(Box.createVerticalStrut(6));

        rId = new JLabel("");
        rId.setFont(YH); rId.setForeground(TEXT_SUB);
        headText.add(rId);
        head.add(headText, BorderLayout.CENTER);
        content.add(head);
        content.add(Box.createVerticalStrut(26));
        content.add(detailDivider());
        content.add(Box.createVerticalStrut(18));

        content.add(sectionLabel("Profile"));
        content.add(Box.createVerticalStrut(8));

        rRemark = detailRow("Remark", "");
        content.add(rRemark); content.add(Box.createVerticalStrut(8));
        rWork = detailRow("Workplace", "");
        content.add(rWork); content.add(Box.createVerticalStrut(8));
        rHome = detailRow("Hometown", "");
        content.add(rHome); content.add(Box.createVerticalStrut(8));
        rSig = detailRow("Signature", "");
        content.add(rSig); content.add(Box.createVerticalStrut(18));
        content.add(detailDivider());
        content.add(Box.createVerticalStrut(18));

        content.add(sectionLabel("More"));
        content.add(Box.createVerticalStrut(8));
        rMutual = detailRow("Mutual Friends", "0");
        content.add(rMutual);
        content.add(Box.createVerticalStrut(24));

        rActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rActionPanel.setOpaque(false);
        rActionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(rActionPanel);

        content.add(Box.createVerticalStrut(24));
        rExtraPanel = new JPanel(new BorderLayout());
        rExtraPanel.setOpaque(false);
        rExtraPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rExtraPanel.setMaximumSize(new Dimension(430, 360));
        content.add(rExtraPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel contentWrap = new JPanel(new GridBagLayout());
        contentWrap.setOpaque(false);
        contentWrap.add(content, gbc);

        JScrollPane sp = new JScrollPane(contentWrap);
        sp.setBorder(null); sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel wrap2 = new JPanel(new BorderLayout());
        wrap2.setOpaque(false);
        wrap2.add(sp, BorderLayout.CENTER);
        return wrap2;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(YHB);
        label.setForeground(TEXT_SUB);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JSeparator detailDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(DIVIDER);
        sep.setMaximumSize(new Dimension(430, 1));
        return sep;
    }

    private JPanel detailRow(String label, String val) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(430, 76));
        JLabel l = new JLabel(label);
        l.setFont(YH); l.setForeground(TEXT_SUB);
        l.setPreferredSize(new Dimension(120, 32));
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

    /** Navigate back from detail view to the previous context */
    private void navigateBack() {
        if (navIdx == 1) {
            // From friend detail back to friends list
            showEmptyRight();
        } else if (navIdx == 3) {
            // From search user detail back to search results
            showSearchRight();
        } else {
            // Default: go back to profile right
            showProfileRight();
        }
    }

    void refreshProfile() {
        User u = network.getCurrentUser();
        if (u == null) return;
        ((AvatarLabel) pAvatar).user = u;
        pAvatar.repaint();
        pName.setText(u.getName());
        pId.setText("@" + u.getUserId());
        pNameF.setText(u.getName());
        pSigArea.setText(u.getSignature());
        pWorkF.setText(u.getWorkplace());
        pHomeF.setText(u.getHometown());
        rFriends.setText(String.valueOf(u.getFriends().size()));
        rPosts.setText(String.valueOf(u.getPosts().size()));
        refreshProfilePosts();
    }

    private void refreshProfilePosts() {
        if (profilePostFeed == null) return;
        profilePostFeed.removeAll();
        User cur = network.getCurrentUser();
        if (cur == null) return;

        List<Post> ownPosts = new ArrayList<>(cur.getPosts());
        ownPosts.sort((p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
        if (ownPosts.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setOpaque(false);
            empty.setPreferredSize(new Dimension(0, 320));
            JLabel label = new JLabel("No posts yet. Share a moment from the Posts tab.");
            label.setFont(YH);
            label.setForeground(TEXT_HINT);
            empty.add(label);
            profilePostFeed.add(empty);
        } else {
            profilePostFeed.add(Box.createVerticalStrut(20));
            for (Post post : ownPosts) {
                profilePostFeed.add(createWidePostCard(post, cur));
                profilePostFeed.add(Box.createVerticalStrut(10));
            }
        }
        profilePostFeed.revalidate();
        profilePostFeed.repaint();
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
            String displayName = cur.getDisplayNameFor(f).toLowerCase();
            if (!query.isEmpty() && !f.getName().toLowerCase().contains(query)
                    && !f.getUserId().toLowerCase().contains(query)
                    && !displayName.contains(query)) continue;
            if ("Same Hometown".equals(fFilter) && !f.getHometown().equalsIgnoreCase(cur.getHometown())) continue;
            if ("Same Workplace".equals(fFilter) && !f.getWorkplace().equalsIgnoreCase(cur.getWorkplace())) continue;
            fModel.addElement(f);
        }
    }

    void refreshMoments() {
        mFeed.removeAll();
        User cur = network.getCurrentUser();
        if (cur == null) return;
        List<Post> posts = getVisiblePostsFor(cur);
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

    private List<Post> getVisiblePostsFor(User user) {
        List<Post> posts = new ArrayList<>(user.getPosts());
        for (User friend : user.getFriends()) {
            posts.addAll(friend.getPosts());
        }
        posts.sort((p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
        return posts;
    }

    void refreshSearch() {
        sModel.clear();
        sRecommendationReasons.clear();
        User cur = network.getCurrentUser();
        if (cur == null) return;
        List<User> recommendations = buildSearchRecommendations(cur);
        if (recommendations.isEmpty()) {
            return;
        }
        for (User u : recommendations) sModel.addElement(u);
    }

    private List<User> buildSearchRecommendations(User cur) {
        List<User> candidates = new ArrayList<>();
        Set<User> existingFriends = new HashSet<>(cur.getFriends());

        for (User user : network.getAllUsers()) {
            if (user.equals(cur) || existingFriends.contains(user)) continue;
            int mutualCount = cur.getMutualFriends(user).size();
            boolean sameWorkplace = sameMeaningfulText(cur.getWorkplace(), user.getWorkplace());
            boolean sameHometown = sameMeaningfulText(cur.getHometown(), user.getHometown());

            if ("Mutual Friends".equals(sFilter)) {
                if (mutualCount <= 0) continue;
                candidates.add(user);
                sRecommendationReasons.put(user, mutualCount + " mutual friend(s)");
            } else if ("Same Workplace".equals(sFilter)) {
                if (!sameWorkplace) continue;
                candidates.add(user);
                String reason = "Same workplace: " + user.getWorkplace();
                if (mutualCount > 0) reason += " | " + mutualCount + " mutual";
                sRecommendationReasons.put(user, reason);
            } else if ("Same Hometown".equals(sFilter)) {
                if (!sameHometown) continue;
                candidates.add(user);
                String reason = "Same hometown: " + user.getHometown();
                if (mutualCount > 0) reason += " | " + mutualCount + " mutual";
                sRecommendationReasons.put(user, reason);
            }
        }

        candidates.sort((a, b) -> {
            int mutualCompare = Integer.compare(cur.getMutualFriends(b).size(), cur.getMutualFriends(a).size());
            if (mutualCompare != 0) return mutualCompare;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return candidates;
    }

    private boolean sameMeaningfulText(String a, String b) {
        if (a == null || b == null) return false;
        String left = a.trim();
        String right = b.trim();
        if (left.isEmpty() || right.isEmpty()) return false;
        if ("Unknown".equalsIgnoreCase(left) || "Unknown".equalsIgnoreCase(right)) return false;
        return left.equalsIgnoreCase(right);
    }

    // ======== ACTIONS ========
    private void saveProfile() {
        User cur = network.getCurrentUser();
        if (cur == null) return;
        String name = pNameF.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Name cannot be empty.", "SnapTok", JOptionPane.WARNING_MESSAGE); return; }
        cur.setName(name);
        cur.setSignature(pSigArea.getText().trim());
        cur.setWorkplace(pWorkF.getText().trim().isEmpty() ? "Unknown" : pWorkF.getText().trim());
        cur.setHometown(pHomeF.getText().trim().isEmpty() ? "Unknown" : pHomeF.getText().trim());
        refreshProfile();
        LoginPanel.rewriteUsersFile(network);
        mainGUI.saveNetworkNow();
        JOptionPane.showMessageDialog(this, "Profile updated!", "SnapTok", JOptionPane.INFORMATION_MESSAGE);
    }

    private void chooseAvatar() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            User cur = network.getCurrentUser();
            if (cur != null) {
                String relativePath = LoginPanel.copyAvatarToAssets(fc.getSelectedFile().getAbsolutePath());
                if (relativePath.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Could not import that avatar.", "SnapTok", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                cur.setAvatarPath(relativePath);
                LoginPanel.rewriteUsersFile(network);
                mainGUI.saveNetworkNow();
                refreshProfile();
            }
        }
    }

    private void createPost() {
        // Now handled by showPostModal()
        showPostModal();
    }

    private void toggleLike(Post post) {
        User cur = network.getCurrentUser();
        if (cur == null) return;
        if (post.isLikedBy(cur)) post.removeLike(cur); else post.addLike(cur);
        mainGUI.saveNetworkNow();
        refreshMoments();
        refreshProfilePosts();
    }

    private void performSearch() {
        if (searchResultModel == null) return;
        searchResultModel.clear();
        String q = sField.getText().trim().toLowerCase();
        if (q.isEmpty()) return;
        User cur = network.getCurrentUser();
        for (User u : network.getAllUsers()) {
            if (u.equals(cur)) continue;
            if (u.getName().toLowerCase().contains(q) || u.getUserId().toLowerCase().contains(q)) {
                searchResultModel.addElement(u);
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
        User cur = network.getCurrentUser();
        updateDetailPanel(user, cur != null && cur.isFriendWith(user));
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
            refreshProfilePosts();
        }
    }

    void showEmptyRight() {
        rightCards.show(rightPanel, "EMPTY");
    }

    void showSearchRight() {
        rightCards.show(rightPanel, "SEARCH");
        performSearch();
    }

    private void updateFriendFilterChips() {
        for (FilterChip chip : fFilterChips) {
            chip.setActive(chip.getText().equals(fFilter));
        }
    }

    private void updateSearchFilterChips() {
        for (FilterChip chip : sFilterChips) {
            chip.setActive(chip.getText().equals(sFilter));
        }
    }

    private void updateDetailPanel(User user, boolean isFriend) {
        User cur = network.getCurrentUser();
        ((AvatarLabel) rAvatar).user = user;
        rAvatar.repaint();
        setDetailLabel(rRemark, "Remark");
        setDetailLabel(rWork, "Workplace");
        setDetailLabel(rHome, "Hometown");
        setDetailLabel(rSig, "Signature");
        setDetailLabel(rMutual, "Mutual Friends");
        String remark = cur != null ? cur.getFriendRemark(user.getUserId()) : "";
        String displayName = cur != null ? cur.getDisplayNameFor(user) : user.getName();
        rName.setText(displayName);
        rId.setText(remark.isEmpty() ? "@" + user.getUserId() : "Name: " + user.getName() + "   @" + user.getUserId());
        setDetailValue(rRemark, remark.isEmpty() ? "Not set" : remark);
        setDetailValue(rWork, user.getWorkplace());
        setDetailValue(rHome, user.getHometown());
        setDetailValue(rSig, user.getSignature().isEmpty() ? "No signature" : user.getSignature());
        List<User> mutual = cur != null ? cur.getMutualFriends(user) : new ArrayList<>();
        setDetailValue(rMutual, mutual.size() + " person(s)");

        rActionPanel.removeAll();
        if (isFriend) {
            StyledButton remarkBtn = new StyledButton("Edit Remark", false);
            remarkBtn.addActionListener(e -> showRemarkDialog(user));
            rActionPanel.add(remarkBtn);
            StyledButton viewFriends = new StyledButton("View Their Friends", false);
            viewFriends.addActionListener(e -> showFriendsOfFriend(user));
            rActionPanel.add(viewFriends);
            StyledButton removeBtn = new StyledButton("Remove Friend", false);
            removeBtn.addActionListener(e -> {
                if (cur != null && JOptionPane.showConfirmDialog(this, "Remove " + user.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    cur.removeFriend(user);
                    mainGUI.saveNetworkNow();
                    refreshFriends();
                    showEmptyRight();
                }
            });
            rActionPanel.add(removeBtn);
        } else {
            if (cur != null && !cur.isFriendWith(user) && !user.equals(cur)) {
                StyledButton addBtn = new StyledButton("Add Friend", true);
                addBtn.addActionListener(e -> {
                    cur.addFriend(user);
                    mainGUI.saveNetworkNow();
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

    private void showRemarkDialog(User friend) {
        User cur = network.getCurrentUser();
        if (cur == null || friend == null) return;

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit Remark", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HAIRLINE),
                new EmptyBorder(20, 22, 18, 22)));

        JLabel title = new JLabel("Edit Remark");
        title.setFont(YH_LG);
        title.setForeground(TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(6));

        JLabel hint = new JLabel("Leave it empty to show this friend's real name.");
        hint.setFont(YH_SM);
        hint.setForeground(TEXT_HINT);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(hint);
        panel.add(Box.createVerticalStrut(16));

        JTextField field = new JTextField(cur.getFriendRemark(friend.getUserId()));
        field.setFont(YH);
        field.setForeground(TEXT_MAIN);
        field.setCaretColor(BRAND);
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(HAIRLINE, 1),
                new EmptyBorder(9, 12, 9, 12)));
        field.setBackground(Color.WHITE);
        field.setMaximumSize(new Dimension(320, 40));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(field);
        panel.add(Box.createVerticalStrut(18));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        StyledButton cancel = new StyledButton("Cancel", false);
        cancel.addActionListener(e -> dialog.dispose());
        StyledButton save = new StyledButton("Save", true);
        save.addActionListener(e -> {
            String remark = field.getText().trim();
            cur.setFriendRemark(friend.getUserId(), remark);
            LoginPanel.rewriteUsersFile(network);
            mainGUI.saveNetworkNow();
            if (fList != null) fList.repaint();
            updateDetailPanel(friend, true);
            dialog.dispose();
        });
        buttons.add(cancel);
        buttons.add(save);
        panel.add(buttons);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton(save);
        SwingUtilities.invokeLater(field::requestFocusInWindow);
        dialog.setVisible(true);
    }

    private void updatePostDetailPanel(Post post) {
        ((AvatarLabel) rAvatar).user = post.getAuthor();
        rAvatar.repaint();
        rName.setText(post.getAuthor().getName());
        rId.setText(post.getTimestampString());
        setDetailLabel(rRemark, "Type");
        setDetailLabel(rWork, "Author");
        setDetailLabel(rHome, "Time");
        setDetailLabel(rSig, "Content");
        setDetailLabel(rMutual, "Engagement");
        setDetailValue(rRemark, "Post");
        setDetailValue(rWork, post.getAuthor().getName());
        setDetailValue(rHome, post.getTimestampString());
        setDetailValue(rSig, post.getContent());
        setDetailValue(rMutual, post.getLikeCount() + " like(s), " + post.getCommentCount() + " comment(s)");

        rActionPanel.removeAll();
        User cur = network.getCurrentUser();
        if (cur != null) {
            boolean liked = post.isLikedBy(cur);
            StyledButton likeBtn = new StyledButton(liked ? "Unlike" : "Like", liked);
            likeBtn.addActionListener(e -> { toggleLike(post); showPostDetail(post); });
            rActionPanel.add(likeBtn);
        }
        rExtraPanel.removeAll();
        rExtraPanel.add(buildPostCommentsPanel(post), BorderLayout.CENTER);
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
            JLabel label = (JLabel) row.getComponent(1);
            label.setText("<html><body style='width:260px'>"
                    + escapeHtml(softWrapLongWords(val, 28))
                    + "</body></html>");
        }
    }

    private void setDetailLabel(JPanel row, String label) {
        if (row.getComponentCount() >= 1) {
            ((JLabel) row.getComponent(0)).setText(label);
        }
    }

    private JPanel buildPostCommentsPanel(Post post) {
        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        if (post.getLikeCount() > 0) {
            JLabel likers = new JLabel("<html><body style='width:390px'>Liked by: "
                    + escapeHtml(post.getLikerIdsString().replace(",", ", ")) + "</body></html>");
            likers.setFont(YH); likers.setForeground(TEXT_SUB);
            likers.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.add(likers);
            top.add(Box.createVerticalStrut(10));
        }

        JLabel title = new JLabel("Comments");
        title.setFont(YHB);
        title.setForeground(TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(title);
        top.add(Box.createVerticalStrut(8));

        JTextArea input = new JTextArea(2, 20);
        input.setFont(YH);
        input.setForeground(TEXT_MAIN);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setBorder(new EmptyBorder(8, 8, 8, 8));
        input.setBackground(Color.WHITE);
        input.setCaretColor(BRAND);

        JScrollPane inputScroll = new JScrollPane(input);
        inputScroll.setBorder(BorderFactory.createLineBorder(HAIRLINE));
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);
        inputScroll.setPreferredSize(new Dimension(0, 54));

        StyledButton send = new StyledButton("Comment", true);
        send.setPreferredSize(new Dimension(120, 44));
        send.addActionListener(e -> addComment(post, input));

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.setMaximumSize(new Dimension(430, 58));
        inputRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputRow.add(inputScroll, BorderLayout.CENTER);
        inputRow.add(send, BorderLayout.EAST);
        top.add(inputRow);
        wrap.add(top, BorderLayout.NORTH);

        JPanel comments = new JPanel();
        comments.setLayout(new BoxLayout(comments, BoxLayout.Y_AXIS));
        comments.setOpaque(false);
        if (post.getComments().isEmpty()) {
            JLabel empty = new JLabel("No comments yet.");
            empty.setFont(YH);
            empty.setForeground(TEXT_HINT);
            empty.setBorder(new EmptyBorder(12, 0, 12, 0));
            comments.add(empty);
        } else {
            for (Comment comment : post.getComments()) {
                comments.add(createCommentItem(comment));
                comments.add(Box.createVerticalStrut(8));
            }
        }
        JScrollPane commentsScroll = new JScrollPane(comments);
        commentsScroll.setBorder(null);
        commentsScroll.setOpaque(false);
        commentsScroll.getViewport().setOpaque(false);
        commentsScroll.setPreferredSize(new Dimension(420, 120));
        commentsScroll.getVerticalScrollBar().setUnitIncrement(12);
        wrap.add(commentsScroll, BorderLayout.CENTER);

        return wrap;
    }

    private JPanel createCommentItem(Comment comment) {
        JPanel item = new JPanel(new BorderLayout(8, 0));
        item.setOpaque(false);
        item.setBorder(new EmptyBorder(6, 0, 6, 0));

        item.add(new AvatarLabel(32, comment.getAuthor()), BorderLayout.WEST);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        JPanel meta = new JPanel(new BorderLayout());
        meta.setOpaque(false);
        JLabel author = new JLabel(comment.getAuthor().getName());
        author.setFont(YHB);
        author.setForeground(TEXT_MAIN);
        meta.add(author, BorderLayout.WEST);
        JLabel time = new JLabel(comment.getTimestampString());
        time.setFont(YH_XS);
        time.setForeground(TEXT_HINT);
        meta.add(time, BorderLayout.EAST);
        body.add(meta);

        JLabel text = new JLabel("<html><body style='width:340px'>"
                + escapeHtml(comment.getContent()) + "</body></html>");
        text.setFont(YH);
        text.setForeground(TEXT_MAIN);
        body.add(text);

        item.add(body, BorderLayout.CENTER);
        return item;
    }

    private void addComment(Post post, JTextArea input) {
        User cur = network.getCurrentUser();
        if (cur == null || post == null) return;
        String text = input.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Write a comment first.", "SnapTok", JOptionPane.WARNING_MESSAGE);
            return;
        }
        post.addComment(cur, text);
        input.setText("");
        mainGUI.saveNetworkNow();
        refreshMoments();
        refreshProfilePosts();
        showPostDetail(post);
    }

    // ======== POST CARD ========
    private JPanel createPostCard(Post post, User cur) {
        return createPostCard(post, cur, 220, 280, 150);
    }

    private JPanel createWidePostCard(Post post, User cur) {
        return createPostCard(post, cur, 520, Integer.MAX_VALUE, 160);
    }

    private JTextArea createWrappedPostText(String text, int width, int maxHeight) {
        JTextArea area = new JTextArea(softWrapLongWords(text, 32));
        area.setFont(YH);
        area.setForeground(TEXT_MAIN);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        area.setSize(new Dimension(width, Short.MAX_VALUE));
        int height = Math.min(Math.max(area.getPreferredSize().height, 24), maxHeight);
        area.setPreferredSize(new Dimension(width, height));
        area.setMaximumSize(new Dimension(width, height));
        area.setToolTipText(text);
        return area;
    }

    private JPanel createPostCard(Post post, User cur, int contentWidth, int maxWidth, int maxHeight) {
        int cardWidth = maxWidth == Integer.MAX_VALUE
                ? contentWidth + 42
                : Math.max(contentWidth + 42, maxWidth);
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(CANVAS);
        // Apple utility card: 1px hairline border, 18px radius (rounded.lg)
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(HAIRLINE, 1),
                new EmptyBorder(14, 14, 14, 14)));
        card.setMaximumSize(new Dimension(cardWidth, maxHeight + 150));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header: fixed-size avatar + meta
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.setMaximumSize(new Dimension(cardWidth, 42));
        AvatarLabel avatar = new AvatarLabel(34, post.getAuthor());
        avatar.setPreferredSize(new Dimension(34, 34));
        avatar.setMinimumSize(new Dimension(34, 34));
        top.add(avatar, BorderLayout.WEST);

        JPanel meta = new JPanel();
        meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
        meta.setOpaque(false);
        JLabel author = new JLabel(post.getAuthor().getName());
        author.setFont(YHB); author.setForeground(TEXT_MAIN);
        meta.add(author);
        JLabel time = new JLabel(post.getTimestampString());
        time.setFont(YH_XS);
        time.setForeground(TEXT_HINT);
        meta.add(time);
        top.add(meta, BorderLayout.CENTER);
        card.add(top);
        card.add(Box.createVerticalStrut(8));

        // Content
        JTextArea content = createWrappedPostText(post.getContent(), contentWidth, maxHeight);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(content);
        card.add(Box.createVerticalStrut(8));

        // Action bar: heart + comment count + view
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bottom.setOpaque(false);
        bottom.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottom.setMaximumSize(new Dimension(cardWidth, 28));

        boolean liked = cur != null && post.isLikedBy(cur);
        HeartIcon heart = new HeartIcon(liked, post);
        heart.setAlignmentY(Component.CENTER_ALIGNMENT);
        bottom.add(heart);

        JLabel likeCount = new JLabel(String.valueOf(post.getLikeCount()));
        likeCount.setFont(YH_SM);
        likeCount.setForeground(TEXT_HINT);
        likeCount.setAlignmentY(Component.CENTER_ALIGNMENT);
        bottom.add(likeCount);

        JLabel commentBtn = new JLabel("\uD83D\uDCAC " + post.getCommentCount());
        commentBtn.setFont(YH_SM);
        commentBtn.setForeground(TEXT_HINT);
        commentBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        commentBtn.setAlignmentY(Component.CENTER_ALIGNMENT);
        commentBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { showPostDetail(post); }
        });
        bottom.add(commentBtn);

        JLabel viewLabel = new JLabel("View \u203A");
        viewLabel.setFont(YH); viewLabel.setForeground(BRAND);
        viewLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        viewLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { showPostDetail(post); }
        });
        bottom.add(viewLabel);
        card.add(bottom);

        // Inline comments (show first 3)
        if (!post.getComments().isEmpty()) {
            card.add(Box.createVerticalStrut(6));
            JPanel commentsArea = new JPanel();
            commentsArea.setLayout(new BoxLayout(commentsArea, BoxLayout.Y_AXIS));
            commentsArea.setOpaque(false);
            commentsArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            int shown = 0;
            for (Comment cmt : post.getComments()) {
                if (shown >= 3) break;
                JPanel cItem = new JPanel(new BorderLayout(6, 0));
                cItem.setOpaque(false);
                cItem.setBorder(new EmptyBorder(2, 0, 2, 0));
                cItem.setMaximumSize(new Dimension(cardWidth, 34));
                AvatarLabel cAv = new AvatarLabel(20, cmt.getAuthor());
                cAv.setPreferredSize(new Dimension(20, 20));
                cAv.setMinimumSize(new Dimension(20, 20));
                cItem.add(cAv, BorderLayout.WEST);
                JLabel cText = new JLabel("<html><body style='width:" + (contentWidth - 30) + "px'><b>"
                        + escapeHtml(cmt.getAuthor().getName()) + "</b> "
                        + escapeHtml(softWrapLongWords(cmt.getContent(), 24)) + "</body></html>");
                cText.setFont(YH_SM);
                cText.setForeground(TEXT_MAIN);
                cItem.add(cText, BorderLayout.CENTER);
                commentsArea.add(cItem);
                shown++;
            }
            if (post.getCommentCount() > 3) {
                JLabel more = new JLabel("View all " + post.getCommentCount() + " comments");
                more.setFont(YH_XS);
                more.setForeground(TEXT_HINT);
                more.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                more.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) { showPostDetail(post); }
                });
                commentsArea.add(more);
            }
            card.add(commentsArea);
        }

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBackground(HOVER_BG); }
            public void mouseExited(MouseEvent e) { card.setBackground(CANVAS); }
            public void mouseClicked(MouseEvent e) { showPostDetail(post); }
        });
        return card;
    }

    // ======== HELPERS ========
    static Image loadAvatarImage(String path, int size) {
        if (path == null || path.isEmpty()) return null;
        String resolved = resolveAvatarPath(path);
        if (resolved == null) resolved = path;
        try {
            Image img = ImageIO.read(new File(resolved));
            return img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        } catch (Exception e) { return null; }
    }

    static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    static String softWrapLongWords(String value, int maxRun) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        int run = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            out.append(ch);
            if (Character.isWhitespace(ch)) {
                run = 0;
            } else {
                run++;
                if (run >= maxRun) {
                    out.append(' ');
                    run = 0;
                }
            }
        }
        return out.toString();
    }

    // ======== INNER CLASSES ========

    /** Thin vertical separator */
    static class JSep extends JPanel {
        JSep() { setPreferredSize(new Dimension(1, 0)); }
        @Override
        protected void paintComponent(Graphics g) { g.setColor(DIVIDER); g.fillRect(0, 0, 1, getHeight()); }
    }

    /** Text field with placeholder hint text */
    static class PlaceholderField extends JTextField {
        private String placeholder;
        PlaceholderField(String placeholder) {
            this.placeholder = placeholder;
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(TEXT_HINT);
                g2.setFont(getFont());
                Insets insets = getInsets();
                g2.drawString(placeholder, insets.left + 2, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                g2.dispose();
            }
        }
    }


    /** Circular avatar label */
    static class AvatarLabel extends JLabel {
        int size; User user;
        AvatarLabel(int size, User user) {
            this.size = size; this.user = user;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Use size field as fallback if component not yet laid out
            int w = getWidth() > 0 ? getWidth() : size;
            int h = getHeight() > 0 ? getHeight() : size;
            int d = Math.min(size, Math.min(w, h));
            if (d <= 0) d = size;
            int x = (w - d) / 2;
            int y = (h - d) / 2;
            Ellipse2D.Double clip = new Ellipse2D.Double(x, y, d, d);

            // Try loading image from avatar path
            boolean imageDrawn = false;
            if (user != null && user.getAvatarPath() != null && !user.getAvatarPath().isEmpty()) {
                try {
                    Image img = loadAvatarImage(user.getAvatarPath(), d);
                    if (img != null) {
                        Shape oldClip = g2.getClip();
                        g2.setClip(clip);
                        g2.drawImage(img, x, y, d, d, this);
                        g2.setClip(oldClip);
                        imageDrawn = true;
                    }
                } catch (Exception e) {
                    imageDrawn = false;
                }
            }

            // Fallback: filled circle with person silhouette
            if (!imageDrawn) {
                g2.setColor(new Color(209, 213, 219));
                g2.fill(clip);
                g2.setColor(Color.WHITE);
                int cx = x + d / 2;
                int cy = y + d / 2;
                // Head - filled circle
                int headD = d / 4;
                int headX = cx - headD / 2;
                int headY = cy - d / 4;
                g2.fillOval(headX, headY, headD, headD);
                // Body - filled arc (half ellipse)
                int bodyW = d / 2;
                int bodyH = d / 3;
                int bodyX = cx - bodyW / 2;
                int bodyY = cy + 2;
                g2.fillArc(bodyX, bodyY, bodyW, bodyH, 0, 180);
            }

            // Border ring — hairline
            g2.setColor(HAIRLINE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(clip);
            g2.dispose();
        }
    }

    /** Nav icon — Apple global nav: white icons, blue selected, dark hover */
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
                }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean sel = (idx == navIdx && idx <= 3);
            // Hover: subtle dark highlight pill
            if (hover && !sel && idx <= 3) {
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillRoundRect(10, 8, 44, 36, 12, 12);
            }
            // Selected: Action Blue icon
            Color c = sel ? BRAND : (hover ? Color.WHITE : new Color(180, 180, 180));
            g2.setColor(c);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = 32, cy = 26;
            if (idx == 0) drawPersonIcon(g2, cx, cy);
            else if (idx == 1) drawFriendsIcon(g2, cx, cy);
            else if (idx == 2) drawMomentsIcon(g2, cx, cy);
            else if (idx == 3) drawSearchIcon(g2, cx, cy);
            // Selected indicator: small blue dot below icon
            if (sel) {
                g2.setColor(BRAND);
                g2.fillOval(cx - 2, 44, 4, 4);
            }
            g2.dispose();
        }
    }

    class NavActionIcon extends JPanel {
        int type;
        boolean hover;

        NavActionIcon(int type, String tip) {
            this.type = type;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(tip);
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (hover) {
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillRoundRect(10, 8, 44, 36, 12, 12);
            }
            g2.setColor(hover ? Color.WHITE : new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = 32, cy = 26;
            if (type == 0) drawSaveIcon(g2, cx, cy);
            else if (type == 1) drawLoadIcon(g2, cx, cy);
            else drawLogoutIcon(g2, cx, cy);
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
        g.drawRect(cx - 10, cy - 10, 20, 20);
        g.drawLine(cx - 5, cy - 10, cx - 5, cy - 3);
        g.drawLine(cx + 5, cy - 10, cx + 5, cy - 3);
        g.drawRect(cx - 6, cy + 2, 12, 8);
    }
    private void drawLoadIcon(Graphics2D g, int cx, int cy) {
        g.drawRoundRect(cx - 10, cy - 4, 20, 14, 4, 4);
        g.drawLine(cx, cy - 14, cx, cy + 2);
        g.drawLine(cx, cy - 14, cx - 6, cy - 8);
        g.drawLine(cx, cy - 14, cx + 6, cy - 8);
    }
    private void drawLogoutIcon(Graphics2D g, int cx, int cy) {
        g.drawArc(cx - 10, cy - 10, 20, 20, 45, 270);
        g.drawLine(cx + 2, cy, cx + 12, cy);
        g.drawLine(cx + 8, cy - 4, cx + 12, cy);
        g.drawLine(cx + 8, cy + 4, cx + 12, cy);
    }

    static class EmptyChatIcon extends JPanel {
        EmptyChatIcon() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(224, 224, 224));
            g2.fillOval(8, 6, 48, 38);
            g2.fillPolygon(new int[]{22, 16, 30}, new int[]{40, 58, 42}, 3);
            g2.setColor(Color.WHITE);
            g2.fillOval(21, 20, 8, 8);
            g2.fillOval(36, 20, 8, 8);

            g2.setColor(new Color(216, 216, 216));
            g2.fillOval(42, 28, 46, 36);
            g2.fillPolygon(new int[]{68, 82, 76}, new int[]{61, 72, 56}, 3);
            g2.setColor(Color.WHITE);
            g2.fillOval(56, 42, 7, 7);
            g2.fillOval(70, 42, 7, 7);
            g2.dispose();
        }
    }

    /** Heart icon for like/unlike with animation */
    class HeartIcon extends JPanel {
        private boolean liked;
        private final Post post;
        private boolean hover;
        private float animProgress = 1f;
        private javax.swing.Timer animTimer;

        HeartIcon(boolean initialLiked, Post post) {
            this.liked = initialLiked;
            this.post = post;
            setOpaque(false);
            setPreferredSize(new Dimension(24, 24));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseClicked(MouseEvent e) {
                    toggleLike(post);
                    liked = !liked;
                    // Animate
                    animProgress = 0f;
                    if (animTimer != null && animTimer.isRunning()) animTimer.stop();
                    animTimer = new javax.swing.Timer(16, null);
                    animTimer.addActionListener(ev -> {
                        animProgress += 0.12f;
                        if (animProgress >= 1f) { animProgress = 1f; animTimer.stop(); }
                        repaint();
                    });
                    animTimer.start();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            // Scale animation: bounce from 0.7 to 1.0
            float scale = 0.7f + 0.3f * Math.min(animProgress, 1f);
            int cx = w / 2, cy = h / 2;
            double s = scale;
            g2.translate(cx, cy);
            g2.scale(s, s);
            g2.translate(-cx, -cy);

            if (liked) {
                g2.setColor(new Color(239, 68, 68)); // red
                drawHeart(g2, w, h, true);
            } else {
                g2.setColor(hover ? new Color(239, 68, 68) : TEXT_HINT);
                drawHeart(g2, w, h, false);
            }
            g2.dispose();
        }

        private void drawHeart(Graphics2D g, int w, int h, boolean fill) {
            int cx = w / 2;
            Path2D.Double heart = new Path2D.Double();
            heart.moveTo(cx, h * 0.85);
            heart.curveTo(cx - w * 0.55, h * 0.55, cx - w * 0.35, h * 0.05, cx, h * 0.3);
            heart.curveTo(cx + w * 0.35, h * 0.05, cx + w * 0.55, h * 0.55, cx, h * 0.85);
            if (fill) g.fill(heart);
            else { g.setStroke(new BasicStroke(1.5f)); g.draw(heart); }
        }
    }

    /** Apple button-primary: flat pill, Action Blue, press scale */
    static class StyledButton extends JButton {
        private boolean primary, hover;
        private boolean pressed;
        StyledButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(YHB); setForeground(primary ? Color.WHITE : BRAND);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(primary ? 220 : 140, primary ? 44 : 36));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
                public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (primary) {
                // Apple: flat fill, darker on press
                g2.setColor(pressed ? BRAND_DARK : BRAND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            } else {
                // Secondary: ghost pill with 1px blue border
                if (hover) { g2.setColor(new Color(0, 102, 204, 12)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); }
                g2.setColor(BRAND); g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Apple configurator-option-chip: pill, hairline border, blue selected */
    class FilterChip extends JButton {
        private boolean active, hover;
        FilterChip(String text, boolean active) {
            super(text);
            this.active = active;
            setFont(YH_SM);
            setForeground(active ? Color.WHITE : TEXT_SUB);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(4, 10, 4, 10));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            setForeground(active ? Color.WHITE : TEXT_SUB);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setColor(BRAND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            } else if (hover) {
                g2.setColor(HOVER_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(HAIRLINE); g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            } else {
                g2.setColor(HAIRLINE); g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Recommendation list renderer */
    class RecommendationCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            User u = (User) value;
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(true);
            row.setBackground(isSelected ? new Color(232, 240, 254) : CANVAS); // Apple blue selection
            row.setBorder(new EmptyBorder(8, 12, 8, 12));

            row.add(new AvatarLabel(42, u), BorderLayout.WEST);

            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setOpaque(false);
            JLabel name = new JLabel(u.getName());
            name.setFont(YHB);
            name.setForeground(TEXT_MAIN);
            info.add(name);

            String reason = sRecommendationReasons.getOrDefault(u, "Recommended");
            JLabel detail = new JLabel(reason);
            detail.setFont(YH_SM);
            detail.setForeground(TEXT_HINT);
            info.add(detail);
            row.add(info, BorderLayout.CENTER);
            return row;
        }
    }

    /** Friend list cell renderer */
    class FriendCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            User u = (User) value;
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(true);
            row.setBackground(isSelected ? new Color(232, 240, 254) : CANVAS); // Apple blue selection
            row.setBorder(new EmptyBorder(8, 12, 8, 12));

            AvatarLabel av = new AvatarLabel(40, u);
            row.add(av, BorderLayout.WEST);

            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setOpaque(false);
            User cur = network.getCurrentUser();
            String remark = cur != null ? cur.getFriendRemark(u.getUserId()) : "";
            String displayName = cur != null ? cur.getDisplayNameFor(u) : u.getName();
            JLabel name = new JLabel(displayName);
            name.setFont(YHB); name.setForeground(TEXT_MAIN);
            info.add(name);
            String latest = "";
            if (!u.getPosts().isEmpty()) {
                Post last = u.getPosts().get(u.getPosts().size() - 1);
                latest = last.getContent();
                if (latest.length() > 20) latest = latest.substring(0, 20) + "...";
            }
            String previewText = !remark.isEmpty() ? "Name: " + u.getName()
                    : (latest.isEmpty() ? u.getWorkplace() : latest);
            JLabel preview = new JLabel(previewText);
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
        try {
            String path = MainGUI.getDefaultNetworkFilePath();
            File dir = new File(path).getParentFile();
            if (!dir.exists()) dir.mkdirs();
            FileManager.saveNetwork(path, network);
            LoginPanel.rewriteUsersFile(network);
            JOptionPane.showMessageDialog(this,
                    "Network saved to:\n" + path,
                    "SnapTok",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
