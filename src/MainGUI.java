import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;

/**
 * Creates the main application window and switches between login and main content screens.
 *
 * @author Team Slayers
 * @version 1.0
 */
public class MainGUI extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private SocialNetwork network;

    private LoginPanel loginPanel;
    private MainContentPanel contentPanel;

    private Point dragStart;

    private Rectangle prevBounds;
    private boolean maximized;

    public static final String LOGIN_CARD = "LOGIN";
    public static final String MAIN_CARD = "MAIN";

    private static final int WIN_W = 1000;
    private static final int WIN_H = 700;

    /**
     * Constructs a new MainGUI object.
     */
    public MainGUI(SocialNetwork network) {
        this.network = network;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(WIN_W, WIN_H);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setTitle("SnapTok");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(true);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                int w = getWidth(), h = getHeight();
                g.setColor(new Color(245, 245, 247));
                if (maximized) { g.fillRect(0, 0, w, h); }
                else {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.fillRoundRect(0, 0, w, h, 16, 16);
                    g2.dispose();
                }
            }
        };
        root.setOpaque(false);

        root.add(buildTitleBar(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setOpaque(false);

        loginPanel = new LoginPanel(this, network);
        contentPanel = new MainContentPanel(this, network);

        mainPanel.add(loginPanel, LOGIN_CARD);
        mainPanel.add(contentPanel, MAIN_CARD);

        root.add(mainPanel, BorderLayout.CENTER);
        add(root);

        applyShape();
        cardLayout.show(mainPanel, LOGIN_CARD);

        autoLoadNetwork();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                autoSaveNetwork();
                LoginPanel.rewriteUsersFile(network);
                dispose();
                System.exit(0);
            }
        });

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                applyShape();
            }
        });
    }

    /**
     * Builds the title bar.
     */
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 40));
        bar.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 0));

        JLabel appName = new JLabel("SnapTok");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        appName.setForeground(new Color(122, 122, 122));
        bar.add(appName, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);

        WinButton minimizeBtn = new WinButton(WinButton.TYPE_MINIMIZE);
        minimizeBtn.addActionListener(e -> setState(JFrame.ICONIFIED));

        WinButton maximizeBtn = new WinButton(WinButton.TYPE_MAXIMIZE);
        maximizeBtn.addActionListener(e -> toggleMaximize());

        WinButton closeBtn = new WinButton(WinButton.TYPE_CLOSE);
        closeBtn.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

        btns.add(minimizeBtn);
        btns.add(maximizeBtn);
        btns.add(closeBtn);
        bar.add(btns, BorderLayout.EAST);

        MouseAdapter dragAdapter = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
            }
            public void mouseDragged(MouseEvent e) {
                if (maximized || dragStart == null) return;
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - dragStart.x,
                        loc.y + e.getY() - dragStart.y);
            }
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) toggleMaximize();
            }
        };
        bar.addMouseListener(dragAdapter);
        bar.addMouseMotionListener(dragAdapter);
        appName.addMouseListener(dragAdapter);

        return bar;
    }

    /**
     * Toggles the maximize.
     */
    private void toggleMaximize() {
        if (maximized) {
            maximized = false;
            setBounds(prevBounds);
        } else {
            prevBounds = getBounds();
            maximized = true;
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle maxBounds = ge.getMaximumWindowBounds();
            setBounds(maxBounds);
        }
        applyShape();
        repaint();
    }

    /**
     * Handles the apply shape operation.
     */
    private void applyShape() {
        if (maximized) {
            setShape(null);
        } else {
            setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 32, 32));
        }
    }

    /**
     * Represents a custom window control button used by the main frame.
     *
     * @author Team Slayers
     * @version 1.0
     */
    static class WinButton extends JButton {
        static final int TYPE_MINIMIZE = 0;
        static final int TYPE_MAXIMIZE = 1;
        static final int TYPE_CLOSE = 2;

        private int type;
        private boolean hover, pressed;

        /**
         * Constructs a new WinButton object.
         */
        WinButton(int type) {
            this.type = type;
            setPreferredSize(new Dimension(32, 32));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
                public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        /**
         * Paints the component.
         */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (pressed) {
                g2.setColor(new Color(224, 224, 224));
                g2.fillOval(2, 2, 28, 28);
            } else if (hover) {
                g2.setColor(new Color(245, 245, 247));
                g2.fillOval(2, 2, 28, 28);
            }

            g2.setColor(new Color(29, 29, 31));
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int cx = 16, cy = 16;
            switch (type) {
                case TYPE_MINIMIZE:
                    g2.drawLine(cx - 5, cy, cx + 5, cy);
                    break;
                case TYPE_MAXIMIZE:
                    g2.drawRect(cx - 5, cy - 5, 10, 10);
                    break;
                case TYPE_CLOSE:
                    g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                    g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
                    break;
            }
            g2.dispose();
        }
    }

    /**
     * Shows the card.
     */
    public void showCard(String cardName) {
        cardLayout.show(mainPanel, cardName);
    }

    /**
     * Shows the main content.
     */
    public void showMainContent() {
        contentPanel.startUserSession();
        cardLayout.show(mainPanel, MAIN_CARD);
        SwingUtilities.invokeLater(() -> contentPanel.showPendingFriendNotifications());
    }

    /**
     * Handles the logout current user operation.
     */
    public void logoutCurrentUser() {
        saveNetworkNow();
        contentPanel.clearSessionState();
        network.setCurrentUser(null);
        loginPanel.clearForLogout();
        cardLayout.show(mainPanel, LOGIN_CARD);
    }

    /**
     * Returns the network.
     */
    public SocialNetwork getNetwork() { return network; }

    private static final String AUTO_SAVE_FILE;
    static {
        AUTO_SAVE_FILE = new File(LoginPanel.getProjectRoot(), "data/network.txt").getAbsolutePath();
    }

    /**
     * Returns the default network file path.
     */
    public static String getDefaultNetworkFilePath() {
        return AUTO_SAVE_FILE;
    }

    /**
     * Saves the network now.
     */
    public void saveNetworkNow() {
        autoSaveNetwork();
        LoginPanel.rewriteUsersFile(network);
    }

    /**
     * Handles the auto load network operation.
     */
    private void autoLoadNetwork() {
        File file = new File(AUTO_SAVE_FILE);
        if (!file.exists()) return;
        try {
            SocialNetwork saved = FileManager.loadNetwork(AUTO_SAVE_FILE);
            for (User u : saved.getAllUsers()) {
                u.setAvatarPath(LoginPanel.normalizeAvatarPath(u.getAvatarPath()));
                if (network.getUser(u.getUserId()) == null) {
                    network.addUser(u);
                } else {

                    User existing = network.getUser(u.getUserId());
                    if (existing.getAvatarPath().isEmpty() && !u.getAvatarPath().isEmpty()) {
                        existing.setAvatarPath(u.getAvatarPath());
                    }
                    for (Post p : u.getPosts()) existing.addPost(p);
                    for (String notification : u.getFriendNotifications()) {
                        existing.addFriendNotification(notification);
                    }
                    for (User f : u.getFriends()) {
                        if (!existing.isFriendWith(f) && network.getUser(f.getUserId()) != null) {
                            existing.addFriend(network.getUser(f.getUserId()));
                        }
                    }
                }
            }

            for (User u : saved.getAllUsers()) {
                User existing = network.getUser(u.getUserId());
                if (existing != null) {
                    for (User f : u.getFriends()) {
                        User friendInNetwork = network.getUser(f.getUserId());
                        if (friendInNetwork != null && !existing.isFriendWith(friendInNetwork)) {
                            existing.addFriend(friendInNetwork);
                        }
                    }
                    for (String notification : u.getFriendNotifications()) {
                        existing.addFriendNotification(notification);
                    }
                }
            }
            if (saved.getPostCounter() > network.getPostCounter()) {
                network.setPostCounter(saved.getPostCounter());
            }
        } catch (Exception e) {

        }
    }

    /**
     * Handles the auto save network operation.
     */
    private void autoSaveNetwork() {
        try {
            File dir = new File(AUTO_SAVE_FILE).getParentFile();
            if (!dir.exists()) dir.mkdirs();
            FileManager.saveNetwork(AUTO_SAVE_FILE, network);
        } catch (Exception e) {

        }
    }
}
