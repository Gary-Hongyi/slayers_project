import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;

/**
 * TikTok-style minimalist login panel.
 * No card layering — content sits directly on the gradient background.
 * Unified input style with floating labels and focus highlight bar.
 */
public class LoginPanel extends JPanel {

    private MainGUI mainGUI;
    private SocialNetwork network;
    private CardLayout internalCards;
    private JPanel internalPanel;

    // Login
    private FloatInput loginIdField;
    private FloatInput loginPassField;
    // Register
    private FloatInput regNameField, regIdField, regPassField, regWorkField, regHomeField;

    private static final String LOGIN = "LOGIN", REGISTER = "REGISTER";

    // Design tokens
    private static final Color BRAND = new Color(59, 130, 246);
    private static final Color BRAND_DARK = new Color(37, 99, 235);
    private static final Color TEXT_MAIN = new Color(51, 65, 85);    // slate-700
    private static final Color TEXT_SUB = new Color(100, 116, 139);  // slate-500
    private static final Color TEXT_HINT = new Color(148, 163, 184); // slate-400
    private static final Color INPUT_BG = new Color(243, 244, 246);  // #f3f4f6
    private static final Font YH = new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 14);
    private static final Font YH_BOLD = new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 14);
    private static final int FIELD_W = 340;
    static final String USERS_FILE;
    static {
        // Locate users.txt in slayers_project/ directory
        String dir = System.getProperty("user.dir");
        File projectDir = new File(dir, "slayers_project");
        if (projectDir.isDirectory()) {
            // Working dir is the parent (e.g. teamwork/)
            USERS_FILE = new File(projectDir, "users.txt").getAbsolutePath();
        } else if (new File(dir).getName().equals("src")) {
            // Working dir is src/, go up one level
            USERS_FILE = new File(dir, ".." + File.separator + "users.txt").getAbsolutePath();
        } else {
            // Already in slayers_project/ or elsewhere
            USERS_FILE = new File(dir, "users.txt").getAbsolutePath();
        }
    }

    public LoginPanel(MainGUI mainGUI, SocialNetwork network) {
        this.mainGUI = mainGUI;
        this.network = network;
        setLayout(new BorderLayout());
        setOpaque(false);

        internalCards = new CardLayout();
        internalPanel = new JPanel(internalCards);
        internalPanel.setOpaque(false);
        internalPanel.add(buildLogin(), LOGIN);
        internalPanel.add(buildRegister(), REGISTER);
        add(internalPanel, BorderLayout.CENTER);

        // Load persisted users on startup
        loadUsersFile();
    }

    // ================================================================
    //  LOGIN SCREEN
    // ================================================================
    private JPanel buildLogin() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        // Title
        JLabel title = centeredLabel("SnapTok",
                new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 32), BRAND);
        form.add(title);
        form.add(Box.createVerticalStrut(8));

        // Subtitle
        form.add(centeredLabel("Sign in to your account", YH, TEXT_HINT));
        form.add(Box.createVerticalStrut(32));

        // User ID
        loginIdField = new FloatInput("User ID", false);
        form.add(loginIdField);
        form.add(Box.createVerticalStrut(16));

        // Password
        loginPassField = new FloatInput("Password", true);
        form.add(loginPassField);
        form.add(Box.createVerticalStrut(4));

        // Forgot password
        JPanel forgotRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        forgotRow.setOpaque(false);
        forgotRow.setMaximumSize(new Dimension(FIELD_W, 20));
        forgotRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        forgotRow.add(linkLabel("Forgot password?", 12));
        form.add(forgotRow);
        form.add(Box.createVerticalStrut(32));

        // Sign in button
        PrimaryButton signIn = new PrimaryButton("Sign in");
        signIn.addActionListener(e -> doLogin());
        form.add(signIn);
        form.add(Box.createVerticalStrut(32));

        // Separator
        form.add(buildSeparator("OR"));
        form.add(Box.createVerticalStrut(24));

        // Register link
        JPanel regRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        regRow.setOpaque(false);
        JLabel noAcc = new JLabel("Don't have an account?  ");
        noAcc.setFont(YH); noAcc.setForeground(TEXT_SUB);
        JLabel regLink = linkLabel("Create account", 14);
        regLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                clearReg();
                internalCards.show(internalPanel, REGISTER);
            }
        });
        regRow.add(noAcc); regRow.add(regLink);
        form.add(regRow);
        form.add(Box.createVerticalStrut(16));

        // Load link
        JLabel loadLink = linkLabel("Load network from file", 12);
        loadLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { loadFile(); }
        });
        form.add(loadLink);

        wrap.add(form);
        return wrap;
    }

    // ================================================================
    //  REGISTER SCREEN
    // ================================================================
    private JPanel buildRegister() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        form.add(centeredLabel("SnapTok",
                new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 32), BRAND));
        form.add(Box.createVerticalStrut(8));
        form.add(centeredLabel("Create your account", YH, TEXT_HINT));
        form.add(Box.createVerticalStrut(32));

        regNameField = new FloatInput("Name", false);
        form.add(regNameField);
        form.add(Box.createVerticalStrut(16));

        regIdField = new FloatInput("Choose a User ID", false);
        form.add(regIdField);
        form.add(Box.createVerticalStrut(16));

        regPassField = new FloatInput("Password", true);
        form.add(regPassField);
        form.add(Box.createVerticalStrut(16));

        regWorkField = new FloatInput("Workplace", false);
        form.add(regWorkField);
        form.add(Box.createVerticalStrut(16));

        regHomeField = new FloatInput("Hometown", false);
        form.add(regHomeField);
        form.add(Box.createVerticalStrut(32));

        PrimaryButton createBtn = new PrimaryButton("Create account");
        createBtn.addActionListener(e -> doRegister());
        form.add(createBtn);
        form.add(Box.createVerticalStrut(32));

        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        backRow.setOpaque(false);
        JLabel already = new JLabel("Already have an account?  ");
        already.setFont(YH); already.setForeground(TEXT_SUB);
        JLabel backLink = linkLabel("Sign in", 14);
        backLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                internalCards.show(internalPanel, LOGIN);
            }
        });
        backRow.add(already); backRow.add(backLink);
        form.add(backRow);

        // Scroll wrapper
        JScrollPane sp = new JScrollPane(form);
        sp.setBorder(null); sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        wrap.add(sp);
        return wrap;
    }

    // ================================================================
    //  ACTIONS
    // ================================================================
    private void doLogin() {
        String id = loginIdField.getText().trim();
        String pw = loginPassField.getText();
        if (id.isEmpty()) { err("Please enter your User ID."); return; }
        if (pw.isEmpty()) { err("Please enter your password."); return; }

        // First check in-memory network, then check users.txt
        User u = network.getUser(id);
        if (u == null) {
            // Try loading from users.txt
            loadUsersFile();
            u = network.getUser(id);
        }
        if (u == null) {
            JOptionPane.showMessageDialog(this, "Invalid User ID or Password",
                    "SnapTok", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!u.getPassword().equals(pw)) {
            JOptionPane.showMessageDialog(this, "Invalid User ID or Password",
                    "SnapTok", JOptionPane.WARNING_MESSAGE);
            loginPassField.setText("");
            return;
        }
        network.setCurrentUser(u);
        mainGUI.showMainContent();
    }

    private void doRegister() {
        String name = regNameField.getText().trim();
        String id = regIdField.getText().trim();
        String pw = regPassField.getText();
        String work = regWorkField.getText().trim();
        String home = regHomeField.getText().trim();
        if (name.isEmpty()) { err("Please enter your name."); return; }
        if (id.isEmpty()) { err("Please choose a User ID."); return; }
        if (id.contains(",") || id.contains(" ")) { err("User ID cannot contain spaces or commas."); return; }
        if (pw.isEmpty()) { err("Please create a password."); return; }

        // Check both in-memory and file
        loadUsersFile();
        if (network.getUser(id) != null) {
            JOptionPane.showMessageDialog(this, "User ID already exists",
                    "SnapTok", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String workplace = work.isEmpty() ? "Unknown" : work;
        String hometown = home.isEmpty() ? "Unknown" : home;

        User newUser = new User(id, name, workplace, hometown, pw);
        network.addUser(newUser);

        // Persist to users.txt
        saveUserToFile(id, pw, name, workplace, hometown);

        JOptionPane.showMessageDialog(this, "Account created successfully",
                "SnapTok", JOptionPane.INFORMATION_MESSAGE);
        clearReg();
        // Switch to login screen
        internalCards.show(internalPanel, LOGIN);
    }

    private void loadFile() {
        JFileChooser c = new JFileChooser();
        if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                MainContentPanel.loadNetworkInto(network, c.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Network loaded!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearReg() {
        regNameField.setText(""); regIdField.setText("");
        regPassField.setText(""); regWorkField.setText("");
        regHomeField.setText("");
    }

    private void err(String msg) {
        JOptionPane.showMessageDialog(this, msg, "SnapTok", JOptionPane.WARNING_MESSAGE);
    }

    // ================================================================
    //  USERS.TXT PERSISTENCE
    // ================================================================

    /** Loads users from users.txt into the network. Creates file if missing. */
    private void loadUsersFile() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { /* ignore */ }
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 5) {
                    String uid = parts[0].trim();
                    String pw = parts[1].trim();
                    String name = parts[2].trim();
                    String work = parts[3].trim();
                    String home = parts[4].trim();
                    if (network.getUser(uid) == null) {
                        User newUser = new User(uid, name, work, home, pw);
                        if (parts.length >= 6) newUser.setSignature(parts[5].trim());
                        if (parts.length >= 7) newUser.setAvatarPath(parts[6].trim());
                        network.addUser(newUser);
                    }
                }
            }
        } catch (IOException e) {
            /* silently ignore read errors */
        }
    }

    /** Appends a new user record to users.txt. */
    private void saveUserToFile(String uid, String pw, String name, String work, String home) {
        // Just rewrite the whole file to keep it consistent
        rewriteUsersFile(network);
    }

    /** Rewrites users.txt with all users currently in the network. */
    static void rewriteUsersFile(SocialNetwork network) {
        try (FileWriter fw = new FileWriter(USERS_FILE, false);
             PrintWriter pw = new PrintWriter(fw)) {
            for (User u : network.getAllUsers()) {
                pw.println(u.getUserId() + "," + u.getPassword() + "," + u.getName()
                        + "," + u.getWorkplace() + "," + u.getHometown()
                        + "," + u.getSignature() + "," + u.getAvatarPath());
            }
        } catch (IOException e) {
            /* silently ignore write errors */
        }
    }

    // ================================================================
    //  UI HELPERS
    // ================================================================

    private JLabel centeredLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font); l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JLabel linkLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, size));
        l.setForeground(BRAND);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { l.setForeground(BRAND_DARK); }
            public void mouseExited(MouseEvent e) { l.setForeground(BRAND); }
        });
        return l;
    }

    private JPanel buildSeparator(String text) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(FIELD_W, 20));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        JSeparator sL = new JSeparator(); sL.setForeground(new Color(226, 232, 240));
        JSeparator sR = new JSeparator(); sR.setForeground(new Color(226, 232, 240));
        JLabel t = new JLabel(text, SwingConstants.CENTER);
        t.setFont(YH); t.setForeground(TEXT_HINT);
        row.add(sL, BorderLayout.WEST); row.add(t, BorderLayout.CENTER);
        row.add(sR, BorderLayout.EAST);
        return row;
    }

    // ================================================================
    //  FLOAT INPUT — 50px, 12px radius, #f3f4f6 fill, focus blue bar
    // ================================================================

    static class FloatInput extends JPanel {
        private JTextField field;
        private JLabel label;
        private boolean focused, hasText, isPassword;
        private boolean passVisible;

        FloatInput(String labelText, boolean password) {
            this.isPassword = password;
            setLayout(null);
            setOpaque(false);
            setPreferredSize(new Dimension(FIELD_W, 50));
            setMaximumSize(new Dimension(FIELD_W, 50));
            setAlignmentX(Component.CENTER_ALIGNMENT);

            if (password) {
                JPasswordField pf = new JPasswordField();
                pf.setEchoChar('\u25CF');
                this.field = pf;
            } else {
                this.field = new JTextField();
            }

            field.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 14));
            field.setForeground(TEXT_MAIN);
            field.setCaretColor(BRAND);
            field.setOpaque(false);
            field.setBorder(BorderFactory.createEmptyBorder(18, 16, 6, password ? 44 : 16));
            field.setBounds(0, 0, FIELD_W, 50);
            field.setBackground(INPUT_BG);

            // Overlay label
            label = new JLabel(labelText);
            label.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 14));
            label.setForeground(TEXT_HINT);
            label.setBounds(16, 15, FIELD_W - 60, 20);

            field.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    focused = true;
                    if (!hasText) animateLabel(true);
                    repaint();
                }
                public void focusLost(FocusEvent e) {
                    focused = false;
                    hasText = !getText().isEmpty();
                    if (!hasText) animateLabel(false);
                    repaint();
                }
            });

            add(label);
            add(field);

            // Password eye icon — added FIRST, then Z-order set to top
            if (password) {
                JPanel eye = new JPanel(null) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        Color c = passVisible ? BRAND : TEXT_HINT;
                        g2.setColor(c);
                        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int cx = 12, cy = 12;
                        if (passVisible) {
                            // Open eye (visible) — show password
                            g2.drawOval(cx - 8, cy - 5, 16, 10);
                            g2.fillOval(cx - 3, cy - 3, 6, 6);
                        } else {
                            // Closed eye (hidden) — password masked
                            g2.drawOval(cx - 8, cy - 5, 16, 10);
                            g2.fillOval(cx - 3, cy - 3, 6, 6);
                            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            g2.drawLine(cx - 9, cy + 7, cx + 9, cy - 7);
                        }
                        g2.dispose();
                    }
                    @Override
                    public Dimension getPreferredSize() { return new Dimension(28, 28); }
                };
                eye.setBounds(FIELD_W - 40, 11, 28, 28);
                eye.setOpaque(false);
                eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                eye.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        passVisible = !passVisible;
                        if (field instanceof JPasswordField) {
                            ((JPasswordField) field).setEchoChar(passVisible ? (char) 0 : '\u25CF');
                            field.repaint();
                        }
                        eye.repaint();
                    }
                });
                add(eye);
                // CRITICAL: ensure eye is on TOP of Z-order so it receives mouse events
                setComponentZOrder(eye, 0);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background fill
            g2.setColor(focused ? Color.WHITE : INPUT_BG);
            g2.fillRoundRect(0, 0, getWidth(), 50, 12, 12);

            // Focus: blue bottom bar
            if (focused) {
                g2.setColor(BRAND);
                g2.fillRoundRect(0, 47, getWidth(), 3, 3, 3);
            }
            g2.dispose();
            super.paintComponent(g);
        }

        private void animateLabel(boolean up) {
            Timer timer = new Timer(10, null);
            final int[] step = {0};
            final int startY = label.getY();
            final int targetY = up ? 4 : 15;
            timer.addActionListener(e -> {
                step[0]++;
                float p = Math.min(step[0] / 12f, 1f);
                float ease = 1 - (1 - p) * (1 - p);
                label.setLocation(label.getX(), (int) (startY + (targetY - startY) * ease));
                float sz = up ? 14f + (11f - 14f) * ease : 11f + (14f - 11f) * ease;
                label.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, Math.max(11, (int) sz)));
                label.setForeground(up ? BRAND : TEXT_HINT);
                if (p >= 1f) timer.stop();
            });
            timer.start();
        }

        public String getText() {
            if (field instanceof JPasswordField)
                return new String(((JPasswordField) field).getPassword());
            return field.getText();
        }

        public void setText(String t) { field.setText(t); }
    }

    // ================================================================
    //  PRIMARY BUTTON — #3b82f6 fill, 50px, 12px radius, hover lift
    // ================================================================

    static class PrimaryButton extends JButton {
        private boolean hover;
        private int lift;

        PrimaryButton(String text) {
            super(text);
            setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 16));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(FIELD_W, 50));
            setMaximumSize(new Dimension(FIELD_W, 50));
            setAlignmentX(Component.CENTER_ALIGNMENT);

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; animateLift(true); }
                public void mouseExited(MouseEvent e) { hover = false; animateLift(false); }
            });
        }

        private void animateLift(boolean up) {
            Timer t = new Timer(10, null);
            final int[] step = {0};
            final int start = lift;
            final int target = up ? -2 : 0;
            t.addActionListener(e -> {
                step[0]++;
                float p = Math.min(step[0] / 10f, 1f);
                float ease = 1 - (1 - p) * (1 - p);
                lift = (int) (start + (target - start) * ease);
                if (p >= 1f) t.stop();
                repaint();
            });
            t.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Shadow on hover
            if (hover) {
                g2.setColor(new Color(59, 130, 246, 40));
                g2.fillRoundRect(2, lift + 4, getWidth() - 4, getHeight() - 2, 14, 14);
            }
            g2.setColor(hover ? BRAND_DARK : BRAND);
            g2.fillRoundRect(0, lift, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
