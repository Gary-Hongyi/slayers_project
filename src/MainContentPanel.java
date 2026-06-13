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
    // Friends right panel card layout
    private CardLayout friendsRightCards;
    private JPanel friendsRightPanel;
    private User detailUser;
    private Post detailPost;

    // Profile
    private JLabel pAvatar, pName, pId;
    private JTextField pNameF, pWorkF, pHomeF;
    private PlaceholderTextArea pSigArea;
    private JPanel profilePostFeed;
    // Friends
    private DefaultListModel<User> fModel;
    private JList<User> fList;
    private JTextField fSearch;
    private String fFilter = "All";
    private final List<FilterChip> fFilterChips = new ArrayList<>();
    private JPanel fListContainer; // CardLayout container for friend list/empty state
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
    private JPanel searchResultsContainer;
    // Right detail labels
    private JLabel rAvatar, rName, rId;
    private JPanel rRemark, rWork, rHome, rSig, rMutual;
    private JLabel rFriends, rPosts;
    private JPanel rActionPanel, rExtraPanel;

    public MainContentPanel(MainGUI mainGUI, SocialNetwork network) {
        this.mainGUI = mainGUI;
        this.network = network;
        setLayout(new BorderLayout());
        setOpaque(false);

        // Left nav bar (64px)
        JPanel navWithSep = new JPanel(new BorderLayout());
        navWithSep.setOpaque(false);
        navWithSep.add(buildNav(), BorderLayout.WEST);
        navWithSep.add(new JSep(), BorderLayout.EAST);
        add(navWithSep, BorderLayout.WEST);

        // Create center panel with CardLayout for different views
        midCards = new CardLayout();
        midPanel = new JPanel(midCards);
        midPanel.setOpaque(false);
        
        // Right panel card layout for detail views (must be built BEFORE friends view
        // so that buildFriendDetailView() overwrites the shared field references)
        rightCards = new CardLayout();
        rightPanel = new JPanel(rightCards);
        rightPanel.setBackground(CANVAS);
        rightPanel.setOpaque(true);
        rightPanel.add(buildEmptyRight(), "EMPTY");
        rightPanel.add(buildDetailRight(), "DETAIL");
        
        // Profile view - 40/60 split
        Component profileView = buildProfileView();
        midPanel.add(profileView, "PROFILE");
        
        // Friends view
        Component friendsView = buildFriendsView();
        midPanel.add(friendsView, "FRIENDS");
        
        // Moments view
        Component momentsView = buildMomentsView();
        midPanel.add(momentsView, "MOMENTS");
        
        // Search view
        Component searchView = buildSearchView();
        midPanel.add(searchView, "SEARCH");

        // Add center area
        JPanel centerAll = new JPanel(new BorderLayout());
        centerAll.setOpaque(false);
        centerAll.add(midPanel, BorderLayout.CENTER);
        add(centerAll, BorderLayout.CENTER);

        midCards.show(midPanel, "PROFILE");
        
        // Initialize profile data
        refreshProfile();
        showProfileRight();
        
        // Force JSplitPane divider location after component is realized
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (profileView instanceof JSplitPane) {
                ((JSplitPane) profileView).setDividerLocation(0.4);
            }
        });
    }

    // ======== LEFT NAV — exact match to HTML sidebar ========
    private JPanel buildNav() {
        // Load nav icon images from image/ folder
        String[] iconFiles = {"icon_profile.png", "icon_friends.png", "icon_memory.png", "icon_search.png"};
        for (int i = 0; i < 4; i++) {
            try {
                // Try multiple paths for flexibility
                java.io.File iconFile = null;
                String[] possiblePaths = {
                    "image/" + iconFiles[i],           // When running from project root
                    "../image/" + iconFiles[i],        // When running from src/
                    "../../image/" + iconFiles[i]      // When running from other directories
                };
                
                for (String path : possiblePaths) {
                    java.io.File testFile = new java.io.File(path);
                    if (testFile.exists()) {
                        iconFile = testFile;
                        break;
                    }
                }
                
                if (iconFile != null && iconFile.exists()) {
                    navIcons[i] = ImageIO.read(iconFile);
                } else {
                    System.err.println("Failed to find icon: " + iconFiles[i]);
                }
            } catch (Exception e) {
                System.err.println("Failed to load icon: " + iconFiles[i] + " - " + e.getMessage());
            }
        }
        
        JPanel nav = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(HOVER_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(HAIRLINE);
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }
        };
        nav.setPreferredSize(new Dimension(64, 0));
        String[] tips = {"Profile", "Friends", "Moments", "Search"};
        for (int i = 0; i < 4; i++) {
            NavIcon ni = new NavIcon(i, tips[i]);
            ni.setBounds(0, 16 + i * 52, 64, 44);
            nav.add(ni);
        }
        return nav;
    }

    private void selectNav(int idx) {
        navIdx = idx;
        repaint();
        switch (idx) {
            case 0: refreshProfile(); midCards.show(midPanel, "PROFILE"); break;
            case 1: refreshFriends(); midCards.show(midPanel, "FRIENDS"); showEmptyRight(); break;
            case 2: refreshMoments(); midCards.show(midPanel, "MOMENTS"); showProfileRight(); break;
            case 3: refreshSearch(); midCards.show(midPanel, "SEARCH"); showSearchRight(); break;
        }
    }

    // ======== PROFILE LEFT — 40% width, personal info panel ========
    private JPanel buildProfileLeft() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(HOVER_BG); // light gray parchment #f5f5f7
        p.setOpaque(true);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(36, 0, 36, 24)); // Normal padding (removed -51px shift)
        content.setPreferredSize(new Dimension(350, 700)); // Increased size to fit avatar + all content

        // Avatar centered — circular with hairline border
        pAvatar = new AvatarLabel(80, null);
        pAvatar.setAlignmentX(Component.CENTER_ALIGNMENT); // Centered below avatar
        pAvatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pAvatar.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { chooseAvatar(); }
        });
        
        // Create wrapper for avatar section with explicit sizing
        JPanel avatarWrap = new JPanel(new BorderLayout());
        avatarWrap.setOpaque(false);
        avatarWrap.setAlignmentX(Component.CENTER_ALIGNMENT); // Centered in BoxLayout
        avatarWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140)); // Increased to fit larger name
        avatarWrap.setBorder(new EmptyBorder(0, -51, 0, 0)); // Apply -51px left shift only to avatar section
        avatarWrap.add(pAvatar, BorderLayout.NORTH);
        
        // Name — bold, 28px, centered
        pName = new JLabel("", SwingConstants.CENTER); // Centered
        pName.setFont(new Font(FONT_FAMILY, Font.BOLD, 28));
        pName.setForeground(TEXT_MAIN);
        pName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); // Increased height for 28px bold font
        avatarWrap.add(pName, BorderLayout.CENTER);
        
        // User ID — muted gray, 17px, centered
        pId = new JLabel("", SwingConstants.CENTER); // Centered
        pId.setFont(new Font(FONT_FAMILY, Font.PLAIN, 17));
        pId.setForeground(TEXT_SUB);
        pId.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        avatarWrap.add(pId, BorderLayout.SOUTH);
        content.add(avatarWrap);
        content.add(Box.createVerticalStrut(24));

        // "SIGNATURE" sub-title label — uppercase, bold, 14px
        JLabel sigLabel = new JLabel("SIGNATURE");
        sigLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
        sigLabel.setForeground(TEXT_SUB);
        sigLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(sigLabel);
        content.add(Box.createVerticalStrut(8));

        // Signature textarea — white bg (matches right panel), hairline border, 10px radius
        pSigArea = new PlaceholderTextArea("Write something about yourself...", 3, 20) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CANVAS); // White background matching right panel
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pSigArea.setFont(new Font(FONT_FAMILY, Font.PLAIN, 17));
        pSigArea.setForeground(TEXT_MAIN);
        pSigArea.setLineWrap(true);
        pSigArea.setWrapStyleWord(true);
        pSigArea.setOpaque(false);
        pSigArea.setBorder(new EmptyBorder(12, 16, 12, 16));
        pSigArea.setCaretColor(BRAND);
        // Wrap in a panel with border instead of JScrollPane
        JPanel sigWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(HAIRLINE); // Light gray border
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        sigWrapper.setOpaque(false);
        sigWrapper.add(pSigArea, BorderLayout.CENTER);
        sigWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        sigWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(sigWrapper);
        content.add(Box.createVerticalStrut(24));

        // Info rows — Name / Workplace / Hometown with bottom hairline dividers
        content.add(buildInfoRow("Name", "Name"));
        content.add(buildInfoRow("Workplace", "Workplace"));
        content.add(buildInfoRow("Hometown", "Hometown"));
        content.add(Box.createVerticalStrut(28));

        // Save Changes — pill-shaped button, not full width
        JButton saveBtn = new JButton("Save Changes") {
            private boolean hover, pressed;
            { setFont(new Font(FONT_FAMILY, Font.BOLD, 17));
              setForeground(Color.WHITE);
              setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
              setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
              addMouseListener(new MouseAdapter() {
                  public void mouseEntered(MouseEvent e) { hover=true; repaint(); }
                  public void mouseExited(MouseEvent e) { hover=false; pressed=false; repaint(); }
                  public void mousePressed(MouseEvent e) { pressed=true; repaint(); }
                  public void mouseReleased(MouseEvent e) { pressed=false; repaint(); }
              });
              addActionListener(e -> saveProfile());
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(pressed ? BRAND_DARK : BRAND);
                int arc = 12; // More rounded pill shape
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose(); super.paintComponent(g);
            }
        };
        saveBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 42)); // Full width
        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(saveBtn);

        p.add(content, BorderLayout.CENTER);
        return p;
    }

    /** HTML info-row: key label (88px, muted) + editable value field, bottom hairline */
    private JPanel buildInfoRow(String labelText, String fieldKey) {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Bottom hairline divider
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(DIVIDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel key = new JLabel(labelText);
        key.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        key.setForeground(TEXT_SUB);
        key.setPreferredSize(new Dimension(88, 20));

        JTextField tf = new JTextField();
        tf.setFont(new Font(FONT_FAMILY, Font.PLAIN, 17));
        tf.setForeground(TEXT_MAIN);
        tf.setOpaque(false);
        tf.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        tf.setCaretColor(BRAND);
        if ("Name".equals(fieldKey)) pNameF = tf;
        else if ("Workplace".equals(fieldKey)) pWorkF = tf;
        else if ("Hometown".equals(fieldKey)) pHomeF = tf;

        row.add(key, BorderLayout.WEST);
        row.add(tf, BorderLayout.CENTER);
        return row;
    }

    // ======== PROFILE RIGHT — exact match to HTML panel-right ========
    private JPanel buildProfileRight() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CANVAS);
        p.setOpaque(true);
        p.setBorder(new EmptyBorder(24, 32, 16, 32));

        // Top: "My Posts" title (left-aligned) + Friends/Posts stats (right-aligned)
        rFriends = new JLabel("0");
        rFriends.setFont(new Font(FONT_FAMILY, Font.BOLD, 28));
        rFriends.setForeground(BRAND);
        rPosts = new JLabel("0");
        rPosts.setFont(new Font(FONT_FAMILY, Font.BOLD, 28));
        rPosts.setForeground(BRAND);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        
        // My Posts title - left aligned, bold 28px
        JLabel title = new JLabel("My Posts");
        title.setFont(new Font(FONT_FAMILY, Font.BOLD, 28));
        title.setForeground(TEXT_MAIN);
        top.add(title, BorderLayout.WEST);

        // Stats row - right aligned
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 32, 0));
        statsRow.setOpaque(false);
        statsRow.add(buildStatBox(rFriends, "Friends"));
        statsRow.add(buildStatBox(rPosts, "Posts"));
        top.add(statsRow, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        // Post feed with empty state
        profilePostFeed = new JPanel();
        profilePostFeed.setLayout(new BoxLayout(profilePostFeed, BoxLayout.Y_AXIS));
        profilePostFeed.setOpaque(false);
        
        // Empty state label — two lines, centered
        JPanel emptyState = new JPanel();
        emptyState.setLayout(new BoxLayout(emptyState, BoxLayout.Y_AXIS));
        emptyState.setOpaque(false);
        emptyState.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel emptyLine1 = new JLabel("No posts yet.", SwingConstants.CENTER);
        emptyLine1.setFont(new Font(FONT_FAMILY, Font.PLAIN, 17));
        emptyLine1.setForeground(TEXT_SUB);
        emptyLine1.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel emptyLine2 = new JLabel("Share a moment from the Posts tab.", SwingConstants.CENTER);
        emptyLine2.setFont(new Font(FONT_FAMILY, Font.PLAIN, 17));
        emptyLine2.setForeground(TEXT_SUB);
        emptyLine2.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        emptyState.add(Box.createVerticalStrut(120));
        emptyState.add(emptyLine1);
        emptyState.add(Box.createVerticalStrut(8));
        emptyState.add(emptyLine2);
        
        profilePostFeed.add(emptyState);
        
        JScrollPane postScroll = new JScrollPane(profilePostFeed);
        postScroll.setBorder(null);
        postScroll.setOpaque(false);
        postScroll.getViewport().setOpaque(false);
        postScroll.getVerticalScrollBar().setUnitIncrement(16);
        postScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(postScroll, BorderLayout.CENTER);

        // Bottom: Logout button — ghost style, 16px from bottom and right edges
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(16, 0, 16, 16)); // 16px padding on right and bottom
        JButton logoutBtn = new JButton("Logout") {
            private boolean hov;
            { setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
              setForeground(BRAND);
              setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
              setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
              addMouseListener(new MouseAdapter() {
                  public void mouseEntered(MouseEvent e) { hov=true; repaint(); }
                  public void mouseExited(MouseEvent e) { hov=false; repaint(); }
              });
              addActionListener(e -> {
                  mainGUI.saveNetworkNow();
                  network.setCurrentUser(null);
                  mainGUI.showCard(MainGUI.LOGIN_CARD);
              });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hov) { g2.setColor(new Color(0,102,204,12)); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8); }
                g2.setColor(BRAND); g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose(); super.paintComponent(g);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(100, 36); }
        };
        bottom.add(logoutBtn);
        
        // hairline top border on bottom panel
        JPanel bottomWithBorder = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(DIVIDER);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        bottomWithBorder.setOpaque(false);
        bottomWithBorder.add(bottom, BorderLayout.CENTER);
        p.add(bottomWithBorder, BorderLayout.SOUTH);

        return p;
    }

    // ======== VIEW WRAPPERS — 40/60 split layouts ========
    
    /** Profile view - left panel (40%) + right panel (60%), no visible divider */
    private Component buildProfileView() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(0); // Hide divider completely
        split.setContinuousLayout(true);
        split.setBorder(null);
        
        // Left side - Profile info (40%)
        JPanel leftProfile = buildProfileLeft();
        split.setLeftComponent(leftProfile);
        
        // Right side - Posts area (60%)
        JPanel rightProfile = buildProfileRight();
        JScrollPane rightScroll = new JScrollPane(rightProfile);
        rightScroll.setBorder(null);
        rightScroll.setOpaque(false);
        rightScroll.getViewport().setOpaque(false);
        rightScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        split.setRightComponent(rightScroll);
        
        // Set divider location after component is realized
        split.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (split.getDividerLocation() <= 0) {
                    split.setDividerLocation(0.4);
                }
            }
        });
        
        return split;
    }
    
    /** Friends view - dual-column layout with JSplitPane */
    private Component buildFriendsView() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(0); // No divider line
        split.setContinuousLayout(true);
        split.setBorder(null);
        
        // Left side - Friend list area (fixed width ~350px)
        JPanel leftFriends = buildFriendsLeft();
        split.setLeftComponent(leftFriends);
        
        // Right side - Friend detail display (adaptive width)
        JPanel rightFriends = buildFriendsRight();
        JScrollPane rightScroll = new JScrollPane(rightFriends);
        rightScroll.setBorder(null);
        rightScroll.setOpaque(false);
        rightScroll.getViewport().setOpaque(false);
        rightScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        split.setRightComponent(rightScroll);
        
        // Set divider location after component is realized
        split.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (split.getDividerLocation() <= 0) {
                    split.setDividerLocation(350);
                }
            }
        });
        
        return split;
    }
    
    /** Moments view - dual-column layout with JSplitPane */
    private Component buildMomentsView() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(0); // No divider line
        split.setContinuousLayout(true);
        split.setBorder(null);
        
        // Left side - Post creation area (fixed width ~350px)
        JPanel leftMoments = buildMomentsLeft();
        split.setLeftComponent(leftMoments);
        
        // Right side - Posts feed display (adaptive width)
        JPanel rightMoments = buildMomentsRight();
        JScrollPane rightScroll = new JScrollPane(rightMoments);
        rightScroll.setBorder(null);
        rightScroll.setOpaque(false);
        rightScroll.getViewport().setOpaque(false);
        rightScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        split.setRightComponent(rightScroll);
        
        // Set divider location after component is realized
        split.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (split.getDividerLocation() <= 0) {
                    split.setDividerLocation(350);
                }
            }
        });
        
        return split;
    }
    
    /** Search view - full width */
    /** Search view - dual-column layout with JSplitPane */
    private Component buildSearchView() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(0); // No divider line
        split.setContinuousLayout(true);
        split.setBorder(null);
        
        // Left side - Recommended users section (fixed width ~350px)
        JPanel leftSearch = buildSearchLeft();
        split.setLeftComponent(leftSearch);
        
        // Right side - Search Users section (adaptive width) - NO outer scroll
        JPanel rightSearch = buildSearchRight();
        split.setRightComponent(rightSearch);
        
        // Set divider location after component is realized
        split.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (split.getDividerLocation() <= 0) {
                    split.setDividerLocation(350);
                }
            }
        });
        
        return split;
    }

    // ======== FRIENDS LEFT PANEL — friend list with search and filters ========
    private JPanel buildFriendsLeft() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(HOVER_BG);
        p.setOpaque(true);
        p.setBorder(new CompoundBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20),
                BorderFactory.createMatteBorder(0, 1, 0, 0, HAIRLINE))); // Divider line on left edge

        JPanel topWrap = new JPanel();
        topWrap.setLayout(new BoxLayout(topWrap, BoxLayout.Y_AXIS));
        topWrap.setOpaque(false);

        // Rounded search input — parchment bg, rounded corners
        fSearch = new PlaceholderField("Search by name...");
        fSearch.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
        fSearch.setForeground(TEXT_MAIN);
        fSearch.setBackground(CANVAS);
        fSearch.setCaretColor(BRAND);
        fSearch.setBorder(new CompoundBorder(
                new RoundedBorder(8, HAIRLINE),
                new EmptyBorder(10, 16, 10, 16)));
        fSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        fSearch.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Wrap search in a panel for proper alignment
        JPanel searchWrapper = new JPanel(new BorderLayout());
        searchWrapper.setOpaque(false);
        searchWrapper.add(fSearch, BorderLayout.CENTER);
        fSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { refreshFriendList(); showEmptyRight(); }
        });
        topWrap.add(searchWrapper);
        topWrap.add(Box.createVerticalStrut(12));

        // Filter chips row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        fFilterChips.clear();
        for (String f : new String[]{"All", "Same Hometown"}) {
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
        topWrap.add(filterRow);
        topWrap.add(Box.createVerticalStrut(12)); // Add spacing after filter row
        p.add(topWrap, BorderLayout.NORTH);

        // Friend list — matching bg with left panel
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
        
        // Empty state panel - shows when no friends
        JPanel emptyState = new JPanel(new GridBagLayout());
        emptyState.setBackground(HOVER_BG);
        emptyState.setOpaque(true);
        
        JLabel emptyText = new JLabel("No friends yet");
        emptyText.setFont(new Font(FONT_FAMILY, Font.PLAIN, 18));
        emptyText.setForeground(TEXT_SUB);
        emptyText.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER; // Center vertically and horizontally
        gbc.insets = new Insets(-112, -30, 0, 0); // Shift up ~112px (moved up 1cm more)
        emptyState.add(emptyText, gbc);
        
        // Card layout to switch between list and empty state
        fListContainer = new JPanel(new CardLayout());
        fListContainer.setBackground(HOVER_BG);
        fListContainer.add(sp, "LIST");
        fListContainer.add(emptyState, "EMPTY");
        
        p.add(fListContainer, BorderLayout.CENTER);
        return p;
    }

    // ======== FRIENDS RIGHT PANEL — friend detail display ========
    private JPanel buildFriendsRight() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CANVAS);
        p.setOpaque(true);
        
        // Use CardLayout to switch between empty state and detail view
        friendsRightCards = new CardLayout();
        friendsRightPanel = new JPanel(friendsRightCards);
        friendsRightPanel.setBackground(CANVAS);
        
        // Empty state - shows "Select a friend"
        JPanel emptyState = new JPanel(new GridBagLayout());
        emptyState.setBackground(CANVAS);
        emptyState.setOpaque(true);
        
        JLabel hint = new JLabel("Select a friend");
        hint.setFont(new Font(FONT_FAMILY, Font.PLAIN, 18));
        hint.setForeground(TEXT_SUB);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(-38, -30, 0, 0);
        emptyState.add(hint, gbc);
        
        friendsRightPanel.add(emptyState, "EMPTY");
        
        // Detail view - reuse the same detail panel structure as Search/Profile
        // Build the detail panel inline here
        JPanel detailView = buildFriendDetailView();
        friendsRightPanel.add(detailView, "DETAIL");
        
        p.add(friendsRightPanel, BorderLayout.CENTER);
        return p;
    }
    
    /** Build the friend detail view panel (similar to search/profile detail) */
    private JPanel buildFriendDetailView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CANVAS);
        panel.setOpaque(true);
        panel.setBorder(new EmptyBorder(24, 32, 16, 32));
        
        // Header with avatar and basic info
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        
        rAvatar = new AvatarLabel(72, null);
        header.add(rAvatar, BorderLayout.WEST);
        
        JPanel headText = new JPanel();
        headText.setLayout(new BoxLayout(headText, BoxLayout.Y_AXIS));
        headText.setOpaque(false);
        
        rName = new JLabel("");
        rName.setFont(new Font(FONT_FAMILY, Font.BOLD, 24));
        rName.setForeground(TEXT_MAIN);
        headText.add(rName);
        
        rId = new JLabel("");
        rId.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
        rId.setForeground(TEXT_SUB);
        headText.add(rId);
        
        header.add(headText, BorderLayout.CENTER);
        panel.add(header, BorderLayout.NORTH);
        
        // Info rows - use detailRow which creates JLabel + JLabel structure
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        rRemark = detailRow("Remark", "Not set");
        rWork = detailRow("Workplace", "Not set");
        rHome = detailRow("Hometown", "Not set");
        rSig = detailRow("Signature", "No signature");
        rMutual = detailRow("Mutual Friends", "0 person(s)");
        
        infoPanel.add(rRemark);
        infoPanel.add(rWork);
        infoPanel.add(rHome);
        infoPanel.add(rSig);
        infoPanel.add(rMutual);
        
        panel.add(infoPanel, BorderLayout.CENTER);
        
        // Action buttons
        rActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        rActionPanel.setOpaque(false);
        rActionPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        
        // Extra panel for showing friend lists, etc.
        rExtraPanel = new JPanel(new BorderLayout());
        rExtraPanel.setOpaque(false);
        rExtraPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rExtraPanel.setMaximumSize(new Dimension(430, 360));
        
        // Combine action panel and extra panel in a vertical layout
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.add(rActionPanel);
        bottomPanel.add(Box.createVerticalStrut(8));
        bottomPanel.add(rExtraPanel);
        
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    // ======== MOMENTS LEFT PANEL — post creation area ========
    private JPanel buildMomentsLeft() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(HOVER_BG);
        p.setOpaque(true);
        p.setBorder(new CompoundBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20),
                BorderFactory.createMatteBorder(0, 1, 0, 0, HAIRLINE))); // Divider line on left edge

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        // "SHARE A MOMENT" sub-title — uppercase, bold, 14px
        JLabel subTitle = new JLabel("SHARE A MOMENT");
        subTitle.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
        subTitle.setForeground(TEXT_SUB);
        subTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        topSection.add(subTitle);
        topSection.add(Box.createVerticalStrut(12));

        // Compose textarea — parchment bg, 10px radius, with placeholder
        mInput = new JTextArea(3, 20) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CANVAS); // White background
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
                
                // Draw placeholder text when empty and not focused
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g3 = (Graphics2D) g.create();
                    g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g3.setFont(getFont());
                    g3.setColor(TEXT_HINT);
                    g3.drawString("What's on your mind?", getInsets().left, getBaseline(getWidth(), getHeight()));
                    g3.dispose();
                }
            }
            
            @Override protected void paintBorder(Graphics g) {
                // No border - we draw our own rounded rect in paintComponent
            }
            
            @Override public void setText(String t) {
                super.setText(t);
                repaint();
            }
        };
        mInput.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
        mInput.setForeground(TEXT_MAIN);
        mInput.setLineWrap(true);
        mInput.setWrapStyleWord(true);
        mInput.setOpaque(false);
        mInput.setBorder(new EmptyBorder(12, 16, 12, 16));
        mInput.setCaretColor(BRAND);
        JScrollPane inputScroll = new JScrollPane(mInput);
        inputScroll.setBorder(null);
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);
        inputScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        inputScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        topSection.add(inputScroll);
        topSection.add(Box.createVerticalStrut(12));

        // "Post" button — full width, pill, blue
        JButton postBtn = new JButton("Post") {
            private boolean hover, pressed;
            { setFont(new Font(FONT_FAMILY, Font.BOLD, 17));
              setForeground(Color.WHITE);
              setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
              setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
              addMouseListener(new MouseAdapter() {
                  public void mouseEntered(MouseEvent e) { hover=true; repaint(); }
                  public void mouseExited(MouseEvent e) { hover=false; pressed=false; repaint(); }
                  public void mousePressed(MouseEvent e) { pressed=true; repaint(); }
                  public void mouseReleased(MouseEvent e) { pressed=false; repaint(); }
              });
              addActionListener(e -> {
                  User cur = network.getCurrentUser();
                  if (cur == null) return;
                  String text = mInput.getText().trim();
                  if (text.isEmpty()) { showStyledDialog("Write something first."); return; }
                  network.createPost(cur, text);
                  mInput.setText("");
                  mainGUI.saveNetworkNow();
                  refreshMoments();
                  refreshProfile();
              });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(pressed ? BRAND_DARK : BRAND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose(); super.paintComponent(g);
            }
        };
        postBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        postBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 48));
        postBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        topSection.add(postBtn);
        topSection.add(Box.createVerticalStrut(20));

        // Hairline divider
        JPanel divLine = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(HAIRLINE); g.fillRect(0,0,getWidth(),1);
            }
        };
        divLine.setOpaque(false);
        divLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divLine.setPreferredSize(new Dimension(0, 1));
        topSection.add(divLine);

        p.add(topSection, BorderLayout.NORTH);

        // Empty state - shows when no posts
        JPanel emptyState = new JPanel(new GridBagLayout());
        emptyState.setBackground(HOVER_BG);
        emptyState.setOpaque(true);
        
        JLabel emptyText = new JLabel("No moments yet. Share something!");
        emptyText.setFont(new Font(FONT_FAMILY, Font.PLAIN, 18));
        emptyText.setForeground(TEXT_SUB);
        emptyText.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(-75, -30, 0, 0);
        emptyState.add(emptyText, gbc);
        
        // Card layout to switch between feed and empty state
        mFeed = new JPanel();
        mFeed.setLayout(new BoxLayout(mFeed, BoxLayout.Y_AXIS));
        mFeed.setOpaque(false);
        
        JPanel feedContainer = new JPanel(new CardLayout());
        feedContainer.setBackground(HOVER_BG);
        feedContainer.add(mFeed, "FEED");
        feedContainer.add(emptyState, "EMPTY");
        
        JScrollPane feedSp = new JScrollPane(feedContainer);
        feedSp.setBorder(null); feedSp.setOpaque(false);
        feedSp.getViewport().setOpaque(false);
        feedSp.getVerticalScrollBar().setUnitIncrement(16);
        feedSp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(feedSp, BorderLayout.CENTER);
        
        return p;
    }

    // ======== MOMENTS RIGHT PANEL — posts display ========
    private JPanel buildMomentsRight() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CANVAS);
        p.setOpaque(true);
        
        // Header section with "My Posts" title and stats
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CANVAS);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createEmptyBorder(36, 36, 24, 36));
        
        JLabel title = new JLabel("My Posts");
        title.setFont(new Font(FONT_FAMILY, Font.BOLD, 32));
        title.setForeground(TEXT_MAIN);
        header.add(title, BorderLayout.WEST);
        
        // Stats panel
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 32, 0));
        stats.setOpaque(false);
        
        JPanel friendsStat = new JPanel(new GridLayout(2, 1, 0, 4));
        friendsStat.setOpaque(false);
        JLabel friendsCount = new JLabel("0", SwingConstants.CENTER);
        friendsCount.setFont(new Font(FONT_FAMILY, Font.BOLD, 32));
        friendsCount.setForeground(BRAND);
        JLabel friendsLabel = new JLabel("Friends", SwingConstants.CENTER);
        friendsLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
        friendsLabel.setForeground(TEXT_SUB);
        friendsStat.add(friendsCount);
        friendsStat.add(friendsLabel);
        
        JPanel postsStat = new JPanel(new GridLayout(2, 1, 0, 4));
        postsStat.setOpaque(false);
        JLabel postsCount = new JLabel("0", SwingConstants.CENTER);
        postsCount.setFont(new Font(FONT_FAMILY, Font.BOLD, 32));
        postsCount.setForeground(BRAND);
        JLabel postsLabel = new JLabel("Posts", SwingConstants.CENTER);
        postsLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
        postsLabel.setForeground(TEXT_SUB);
        postsStat.add(postsCount);
        postsStat.add(postsLabel);
        
        stats.add(friendsStat);
        stats.add(postsStat);
        header.add(stats, BorderLayout.EAST);
        
        p.add(header, BorderLayout.NORTH);
        
        // Empty state message in center
        JPanel emptyInner = new JPanel(new GridBagLayout());
        emptyInner.setBackground(CANVAS);
        emptyInner.setOpaque(true);
        
        JLabel hintText = new JLabel("No posts yet. Share a moment from the Posts tab.");
        hintText.setFont(new Font(FONT_FAMILY, Font.PLAIN, 18));
        hintText.setForeground(TEXT_SUB);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(-75, -30, 0, 0);
        emptyInner.add(hintText, gbc);
        
        p.add(emptyInner, BorderLayout.CENTER);
        
        return p;
    }
    // ======== MOMENTS MIDDLE — exact match to HTML panel-left ========

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
            if (text.isEmpty()) { showStyledDialog("Write something first."); return; }
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

    // ======== SEARCH LEFT PANEL — recommended users section ========
    private JPanel buildSearchLeft() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(HOVER_BG);
        p.setOpaque(true);
        p.setBorder(new CompoundBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20),
                BorderFactory.createMatteBorder(0, 1, 0, 0, HAIRLINE))); // Divider line on left edge

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        // "RECOMMENDED FOR YOU" sub-title — uppercase, bold, 14px
        JLabel subTitle = new JLabel("RECOMMENDED FOR YOU");
        subTitle.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
        subTitle.setForeground(TEXT_SUB);
        subTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        topSection.add(subTitle);
        topSection.add(Box.createVerticalStrut(12));

        // Filter chips row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        filterRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        sFilterChips.clear();
        for (String filter : new String[]{"Mutual Friends", "Same Workplace"}) {
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
        topSection.add(filterRow);
        topSection.add(Box.createVerticalStrut(20));

        // Hairline divider
        JPanel divLine = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(HAIRLINE); g.fillRect(0,0,getWidth(),1);
            }
        };
        divLine.setOpaque(false);
        divLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divLine.setPreferredSize(new Dimension(0, 1));
        topSection.add(divLine);

        p.add(topSection, BorderLayout.NORTH);

        // Empty state - shows when no recommendations
        JPanel emptyState = new JPanel(new GridBagLayout());
        emptyState.setBackground(HOVER_BG);
        emptyState.setOpaque(true);
        
        JLabel emptyText = new JLabel("No recommendations yet");
        emptyText.setFont(new Font(FONT_FAMILY, Font.PLAIN, 18));
        emptyText.setForeground(TEXT_SUB);
        emptyText.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(-75, -30, 0, 0);
        emptyState.add(emptyText, gbc);
        
        // Card layout to switch between list and empty state
        sModel = new DefaultListModel<>();
        sList = new JList<>(sModel);
        sList.setCellRenderer(new RecommendationCellRenderer());
        sList.setFixedCellHeight(68);
        sList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sList.setBackground(HOVER_BG); // Match left panel background
        sList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && sList.getSelectedValue() != null) {
                User selected = sList.getSelectedValue();
                showAddFriendDialog(selected);
                // Clear selection so it can be triggered again
                sList.clearSelection();
            }
        });
        JScrollPane sp = new JScrollPane(sList);
        sp.setBorder(null); sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setBackground(HOVER_BG); // Match left panel background
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JPanel listContainer = new JPanel(new CardLayout());
        listContainer.setBackground(HOVER_BG);
        listContainer.add(sp, "LIST");
        listContainer.add(emptyState, "EMPTY");
        
        p.add(listContainer, BorderLayout.CENTER);
        
        return p;
    }

    // ======== SEARCH MIDDLE — exact match to HTML panel-left ========

    // ======== SEARCH RIGHT — exact match to HTML panel-right ========
    private JPanel buildSearchRight() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CANVAS);
        p.setOpaque(true);
        
        // Header section with "Search Users" title
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CANVAS);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createEmptyBorder(36, 36, 24, 36));
        
        JLabel title = new JLabel("Search Users");
        title.setFont(new Font(FONT_FAMILY, Font.BOLD, 32));
        title.setForeground(TEXT_MAIN);
        header.add(title, BorderLayout.WEST);
        
        p.add(header, BorderLayout.NORTH);
        
        // Main content area with search row and results/empty state
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(CANVAS);
        mainContent.setOpaque(true);
        
        // Search row: input field + Go button
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        searchWrap.setOpaque(false);
        searchWrap.setBorder(BorderFactory.createEmptyBorder(0, 36, 24, 36));
        
        sField = new PlaceholderField("Enter User ID or name...");
        sField.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
        sField.setForeground(TEXT_MAIN);
        sField.setCaretColor(BRAND);
        sField.setBorder(new RoundedBorder(24, HAIRLINE));
        sField.setPreferredSize(new Dimension(320, 42));
        sField.setFocusable(true);
        sField.setEditable(true);
        
        // Add action listener for Enter key
        sField.addActionListener(e -> performSearch());
        
        sField.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                sField.requestFocusInWindow();
            }
        });
        sField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { performSearch(); }
        });
        searchWrap.add(sField);
        
        JButton goBtn = new JButton("Go") {
            private boolean hov;
            { setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
              setForeground(BRAND);
              setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
              setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
              addMouseListener(new MouseAdapter() {
                  public void mouseEntered(MouseEvent e) { hov=true; repaint(); }
                  public void mouseExited(MouseEvent e) { hov=false; repaint(); }
              });
              addActionListener(e -> performSearch());
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hov) { g2.setColor(new Color(0,102,204,12)); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8); }
                g2.setColor(BRAND); g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose(); super.paintComponent(g);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(72, 42); }
        };
        searchWrap.add(goBtn);
        mainContent.add(searchWrap, BorderLayout.NORTH);
        
        // Card layout for results list and empty state
        searchResultsContainer = new JPanel(new CardLayout());
        searchResultsContainer.setBackground(CANVAS);
        searchResultsContainer.setOpaque(true);
        
        // Search results list
        searchResultModel = new DefaultListModel<>();
        searchResultList = new JList<>(searchResultModel);
        searchResultList.setCellRenderer(new FriendCellRenderer());
        searchResultList.setFixedCellHeight(64);
        searchResultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultList.setBackground(CANVAS); // Explicitly set list background
        searchResultList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && searchResultList.getSelectedValue() != null) {
                User selected = searchResultList.getSelectedValue();
                // Show add friend confirmation dialog immediately
                showAddFriendDialog(selected);
                // Clear selection so it can be triggered again
                searchResultList.clearSelection();
            }
        });
        JScrollPane resultsSp = new JScrollPane(searchResultList);
        resultsSp.setBorder(null);
        resultsSp.setOpaque(false);
        resultsSp.getViewport().setOpaque(false);
        resultsSp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        searchResultsContainer.add(resultsSp, "RESULTS");
        
        // Empty state message
        JPanel emptyInner = new JPanel(new GridBagLayout());
        emptyInner.setBackground(CANVAS);
        emptyInner.setOpaque(true);
        
        JLabel hintText = new JLabel("Search for users by ID or name");
        hintText.setFont(new Font(FONT_FAMILY, Font.PLAIN, 18));
        hintText.setForeground(TEXT_SUB);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(-75, -30, 0, 0);
        emptyInner.add(hintText, gbc);
        searchResultsContainer.add(emptyInner, "EMPTY");
        
        mainContent.add(searchResultsContainer, BorderLayout.CENTER);
        
        p.add(mainContent, BorderLayout.CENTER);
        
        return p;
    }

    private JPanel buildEmptyRight() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(CANVAS);
        p.setOpaque(true);
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        EmptyChatIcon icon = new EmptyChatIcon();
        icon.setPreferredSize(new Dimension(90, 68));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(icon);
        inner.add(Box.createVerticalStrut(10));
        JLabel hint = new JLabel("Select a friend to start chatting");
        hint.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
        hint.setForeground(TEXT_SUB);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(hint);
        p.add(inner);
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
        
        // Show empty state if no friends match
        CardLayout cl = (CardLayout) fListContainer.getLayout();
        if (fModel.getSize() == 0) {
            cl.show(fListContainer, "EMPTY");
        } else {
            cl.show(fListContainer, "LIST");
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
        
        // Get the listContainer from the left panel
        JPanel listContainer = null;
        Component comp = ((JSplitPane) midPanel.getComponent(3)).getLeftComponent();
        if (comp instanceof JPanel) {
            JPanel leftPanel = (JPanel) comp;
            Component centerComp = leftPanel.getComponent(1); // CENTER component
            if (centerComp instanceof JPanel) {
                listContainer = (JPanel) centerComp;
            }
        }
        
        if (recommendations.isEmpty()) {
            // Show empty state
            if (listContainer != null && listContainer.getLayout() instanceof CardLayout) {
                CardLayout cl = (CardLayout) listContainer.getLayout();
                cl.show(listContainer, "EMPTY");
            }
            return;
        }
        
        // Show list with recommendations
        for (User u : recommendations) sModel.addElement(u);
        if (listContainer != null && listContainer.getLayout() instanceof CardLayout) {
            CardLayout cl = (CardLayout) listContainer.getLayout();
            cl.show(listContainer, "LIST");
        }
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
        if (name.isEmpty()) { showStyledDialog("Name cannot be empty."); return; }
        cur.setName(name);
        cur.setSignature(pSigArea.getText().trim());
        cur.setWorkplace(pWorkF.getText().trim().isEmpty() ? "Unknown" : pWorkF.getText().trim());
        cur.setHometown(pHomeF.getText().trim().isEmpty() ? "Unknown" : pHomeF.getText().trim());
        refreshProfile();
        LoginPanel.rewriteUsersFile(network);
        mainGUI.saveNetworkNow();
        showStyledDialog("Profile updated successfully!");
    }

    private void chooseAvatar() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            User cur = network.getCurrentUser();
            if (cur != null) {
                String relativePath = LoginPanel.copyAvatarToAssets(fc.getSelectedFile().getAbsolutePath());
                if (relativePath.isEmpty()) {
                    showStyledDialog("Could not import that avatar.");
                    return;
                }
                cur.setAvatarPath(relativePath);
                LoginPanel.rewriteUsersFile(network);
                mainGUI.saveNetworkNow();
                refreshProfile();
            }
        }
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
        
        if (q.isEmpty()) {
            if (searchResultsContainer != null) {
                CardLayout cl = (CardLayout) searchResultsContainer.getLayout();
                cl.show(searchResultsContainer, "EMPTY");
            }
            return;
        }
        
        User cur = network.getCurrentUser();
        
        for (User u : network.getAllUsers()) {
            if (u.equals(cur)) continue;
            boolean matchesName = u.getName().toLowerCase().contains(q);
            boolean matchesId = u.getUserId().toLowerCase().contains(q);
            if (matchesName || matchesId) {
                searchResultModel.addElement(u);
            }
        }
        
        if (searchResultsContainer != null) {
            CardLayout cl = (CardLayout) searchResultsContainer.getLayout();
            if (searchResultModel.getSize() > 0) {
                cl.show(searchResultsContainer, "RESULTS");
            } else {
                cl.show(searchResultsContainer, "EMPTY");
            }
        }
    }

    // ======== SHOW DETAIL ========
    void showFriendDetail(User friend) {
        detailUser = friend;
        updateDetailPanel(friend, true);
        if (friendsRightCards != null && friendsRightPanel != null) {
            friendsRightCards.show(friendsRightPanel, "DETAIL");
            friendsRightPanel.revalidate();
            friendsRightPanel.repaint();
        }
    }

    void showSearchUserDetail(User user) {
        detailUser = user;
        User cur = network.getCurrentUser();
        updateDetailPanel(user, cur != null && cur.isFriendWith(user));
    }

    void showPostDetail(Post post) {
        detailPost = post;
        // Update detail panel for post
        updatePostDetailPanel(post);
    }

    void showProfileRight() {
        User cur = network.getCurrentUser();
        if (cur != null) {
            rFriends.setText(String.valueOf(cur.getFriends().size()));
            rPosts.setText(String.valueOf(cur.getPosts().size()));
            refreshProfilePosts();
        }
    }

    void showEmptyRight() {
        // Show EMPTY card in friends right panel when on Friends tab
        if (friendsRightCards != null && friendsRightPanel != null) {
            friendsRightCards.show(friendsRightPanel, "EMPTY");
        }
    }

    void showSearchRight() {
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
            StyledButton viewFriends = new StyledButton("View Friends", false);
            viewFriends.addActionListener(e -> showFriendsDialog(user));
            rActionPanel.add(viewFriends);
            StyledButton removeBtn = new StyledButton("Remove Friend", false);
            removeBtn.addActionListener(e -> showRemoveFriendDialog(user));
            rActionPanel.add(removeBtn);
        } else {
            if (cur != null && !cur.isFriendWith(user) && !user.equals(cur)) {
                StyledButton addBtn = new StyledButton("Add Friend", true);
                addBtn.addActionListener(e -> {
                    // Show confirmation dialog before adding friend
                    String message = "Add <b>" + escapeHtml(user.getName()) + "</b> as your friend?\n<br><span style='color:#7a7a7a'>User ID: " + escapeHtml(user.getUserId()) + "</span>";
                    if (showStyledConfirm(message, "Add Friend")) {
                        cur.addFriend(user);
                        mainGUI.saveNetworkNow();
                        showStyledDialog(user.getName() + " has been added as a friend!");
                        refreshSearch();
                    }
                });
                rActionPanel.add(addBtn);
            }
        }
        rExtraPanel.removeAll();
        rActionPanel.revalidate();
        rActionPanel.repaint();
        rExtraPanel.revalidate(); 
        rExtraPanel.repaint();
    }

    private void showRemoveFriendDialog(User user) {
        User cur = network.getCurrentUser();
        if (cur == null || user == null) return;

        // Create custom styled dialog for remove friend confirmation
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Remove Friend", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HAIRLINE),
                new EmptyBorder(24, 28, 20, 28)));

        JLabel title = new JLabel("Do you want to remove this friend?");
        title.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        title.setForeground(TEXT_MAIN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JButton cancelBtn = new JButton("Cancel") {
            { setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
              setForeground(BRAND); setContentAreaFilled(false);
              setBorderPainted(false); setFocusPainted(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,102,204,12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BRAND); g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose(); super.paintComponent(g);
            }
        };
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton removeBtn = new JButton("Remove") {
            private boolean hov;
            { setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
              setForeground(Color.WHITE); setContentAreaFilled(false);
              setBorderPainted(false); setFocusPainted(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
              addMouseListener(new MouseAdapter() {
                  public void mouseEntered(MouseEvent e) { hov=true; repaint(); }
                  public void mouseExited(MouseEvent e) { hov=false; repaint(); }
              }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? new Color(0,90,185) : BRAND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose(); super.paintComponent(g);
            }
        };
        removeBtn.addActionListener(e -> {
            cur.removeFriend(user);
            mainGUI.saveNetworkNow();
            refreshFriends();
            showEmptyRight();
            dialog.dispose();
        });

        cancelBtn.setPreferredSize(new Dimension(100, 36));
        removeBtn.setPreferredSize(new Dimension(100, 36));

        buttonPanel.add(cancelBtn);
        buttonPanel.add(removeBtn);

        panel.add(title);
        panel.add(buttonPanel);

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
    }

    /** Show add friend confirmation dialog when clicking search result */
    private void showAddFriendDialog(User user) {
        User cur = network.getCurrentUser();
        if (cur == null || user == null) return;
        
        // Check if already friends
        if (cur.isFriendWith(user)) {
            showStyledDialog("You are already friends with " + escapeHtml(user.getName()) + ".");
            return;
        }
        
        String message = "Add <b>" + escapeHtml(user.getName()) + "</b> as your friend?" +
                "\n<br><span style='color:#7a7a7a'>User ID: " + escapeHtml(user.getUserId()) + "</span>";
        
        if (showStyledConfirm(message, "Add Friend")) {
            cur.addFriend(user);
            mainGUI.saveNetworkNow();
            showStyledDialog(escapeHtml(user.getName()) + " has been added as a friend!");
            refreshSearch();
        }
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

    private void showFriendsDialog(User user) {
        User cur = network.getCurrentUser();
        if (cur == null || user == null) return;

        // Create custom styled dialog for viewing friends
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), user.getName() + "'s Friends", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HAIRLINE),
                new EmptyBorder(24, 28, 20, 28)));

        JLabel title = new JLabel(user.getName() + "'s Friends");
        title.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        title.setForeground(TEXT_MAIN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create friends list
        DefaultListModel<User> model = new DefaultListModel<>();
        for (User f : user.getFriends()) model.addElement(f);
        JList<User> list = new JList<>(model);
        list.setCellRenderer(new FriendCellRenderer());
        list.setFixedCellHeight(50);
        list.setPreferredSize(new Dimension(300, 250));
        
        // Add click listener to show add friend dialog for non-friends
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index >= 0 && index < model.getSize()) {
                        User selectedFriend = model.getElementAt(index);
                        // Check if current user is already friends with this person
                        boolean isAlreadyFriend = cur.getFriends().contains(selectedFriend);
                        if (!isAlreadyFriend) {
                            // Show add friend dialog
                            showAddFriendDialog(selectedFriend);
                        } else {
                            // Show message that you're already friends
                            JOptionPane.showMessageDialog(dialog,
                                "You are already friends with " + selectedFriend.getName() + ".",
                                "Already Friends",
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        scrollPane.setPreferredSize(new Dimension(300, 250));

        JPanel closeButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        closeButtonPanel.setOpaque(false);
        StyledButton closeBtn = new StyledButton("Close", false);
        closeBtn.addActionListener(e -> dialog.dispose());
        closeButtonPanel.add(closeBtn);

        panel.add(title);
        panel.add(Box.createVerticalStrut(16));
        panel.add(scrollPane);
        panel.add(closeButtonPanel);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
    }

    private void setDetailValue(JPanel row, String val) {
        if (row != null && row.getComponentCount() >= 2) {
            Component comp = row.getComponent(1);
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                label.setText("<html><body style='width:260px'>" + escapeHtml(softWrapLongWords(val, 28)) + "</body></html>");
            }
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
            showStyledDialog("Write a comment first.");
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

    /** JTextArea with placeholder text support */
    static class PlaceholderTextArea extends JTextArea {
        private String placeholder;
        
        PlaceholderTextArea(String placeholder, int rows, int cols) {
            super(rows, cols);
            this.placeholder = placeholder;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(TEXT_HINT);
                g2.setFont(getFont());
                Insets insets = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int y = insets.top + fm.getAscent();
                g2.drawString(placeholder, insets.left + 2, y);
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

    // Nav icon images loaded from image/ folder
    private BufferedImage[] navIcons = new BufferedImage[4];

    /** Nav icon — parchment sidebar: gray icons, blue active with left bar indicator */
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
            // Hover background — #f0f0f0 divider, 10px radius
            if (hover && !sel) {
                g2.setColor(DIVIDER);
                g2.fillRoundRect(10, 2, 44, 40, 10, 10);
            }
            // Draw icon image centered at (32, 22)
            if (navIcons[idx] != null) {
                int iw = 24, ih = 24; // icon size
                int x = 32 - iw / 2;
                int y = 22 - ih / 2;
                // Apply tint color for selected/hover state
                if (sel) {
                    g2.setColor(BRAND);
                } else {
                    g2.setColor(TEXT_SUB);
                }
                g2.drawImage(navIcons[idx], x, y, iw, ih, null);
            }
            // Active indicator — 3px blue left bar
            if (sel) {
                g2.setColor(BRAND);
                g2.setStroke(new BasicStroke(1f));
                g2.fillRoundRect(0, 9, 3, 26, 2, 2);
            }
            g2.dispose();
        }
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

    /** HTML btn-chip / btn-chip-off: full-pill, blue active, hairline inactive */
    /** Filter chip button - rounded rectangle style */
    class FilterChip extends JButton {
        private boolean active, hover;
        FilterChip(String text, boolean active) {
            super(text);
            this.active = active;
            setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
            setForeground(active ? Color.WHITE : TEXT_MAIN);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(6, 14, 6, 14));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            setForeground(active ? Color.WHITE : TEXT_MAIN);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int radius = 8; // Rounded rectangle corners
            
            if (active) {
                // Active state - solid blue background with border
                g2.setColor(BRAND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                g2.setColor(BRAND_DARK);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            } else {
                // Inactive state - white background with gray border
                if (hover) {
                    g2.setColor(HOVER_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                } else {
                    g2.setColor(CANVAS);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                }
                g2.setColor(HAIRLINE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
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
            String previewText;
            if (!remark.isEmpty()) {
                // Show friend remark with ID
                previewText = "ID: " + u.getUserId();
            } else {
                // Show user ID as default
                previewText = "ID: " + u.getUserId();
            }
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

    // ================================================================
    //  APPLE-STYLE DIALOGS — replaces JOptionPane everywhere
    // ================================================================

    /** Shows a styled info/warning dialog with a single OK button. */
    void showStyledDialog(String message) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "SnapTok",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,20));
                g2.fillRoundRect(0, 2, getWidth(), getHeight(), 16, 16);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight()-2, 16, 16);
                g2.setColor(HAIRLINE); g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-3, 16, 16);
                g2.dispose();
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(26, 32, 22, 32));
        JLabel msg = new JLabel("<html><div style='text-align:center;width:260px;'>" +
                MainContentPanel.escapeHtml(message) + "</div></html>");
        msg.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        msg.setForeground(TEXT_MAIN);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(msg);
        panel.add(Box.createVerticalStrut(20));
        JButton ok = makeDialogButton("OK", true);
        ok.addActionListener(e -> dialog.dispose());
        ok.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(ok);
        dialog.setContentPane(panel);
        dialog.pack(); dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.setVisible(true);
    }

    /** Shows a styled confirm dialog; returns true if user clicks the affirmative button. */
    boolean showStyledConfirm(String message, String confirmLabel) {
        final boolean[] result = {false};
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "SnapTok",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,20));
                g2.fillRoundRect(0, 2, getWidth(), getHeight(), 16, 16);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight()-2, 16, 16);
                g2.setColor(HAIRLINE); g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-3, 16, 16);
                g2.dispose();
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(26, 32, 22, 32));
        JLabel msg = new JLabel("<html><div style='text-align:center;width:240px;'>" +
                message + "</div></html>");
        msg.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        msg.setForeground(TEXT_MAIN);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(msg);
        panel.add(Box.createVerticalStrut(20));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btns.setOpaque(false);
        JButton cancel = makeDialogButton("Cancel", false);
        cancel.addActionListener(e -> dialog.dispose());
        JButton confirm = makeDialogButton(confirmLabel, true);
        confirm.addActionListener(e -> { result[0] = true; dialog.dispose(); });
        btns.add(cancel); btns.add(confirm);
        panel.add(btns);
        dialog.setContentPane(panel);
        dialog.pack(); dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton(confirm);
        dialog.setVisible(true);
        return result[0];
    }

    private JButton makeDialogButton(String text, boolean primary) {
        JButton btn = new JButton(text) {
            private boolean hov, prs;
            {
                setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
                setForeground(primary ? Color.WHITE : BRAND);
                setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
                setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(primary ? 140 : 100, 40));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hov = false; prs = false; repaint(); }
                    public void mousePressed(MouseEvent e) { prs = true; repaint(); }
                    public void mouseReleased(MouseEvent e) { prs = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (primary) {
                    g2.setColor(prs ? BRAND_DARK : (hov ? new Color(0,90,185) : BRAND));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                } else {
                    if (hov) { g2.setColor(new Color(0,102,204,12)); g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16); }
                    g2.setColor(BRAND); g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                }
                g2.dispose(); super.paintComponent(g);
            }
        };
        return btn;
    }

    /** File operations */
    static void loadNetworkInto(SocialNetwork network, java.io.File file) throws Exception {
        SocialNetwork ld = FileManager.loadNetwork(file.getAbsolutePath());
        for (User u : ld.getAllUsers()) network.addUser(u);
        network.setPostCounter(ld.getPostCounter());
        LoginPanel.rewriteUsersFile(network);
    }


    /** Custom rounded border for search field */
    class RoundedBorder extends AbstractBorder {
        private int radius;
        private Color borderColor;
        
        RoundedBorder(int radius, Color borderColor) {
            this.radius = radius;
            this.borderColor = borderColor;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(Math.max(4, radius / 3), Math.max(8, radius / 2), Math.max(4, radius / 3), Math.max(8, radius / 2));
        }
    }
}
