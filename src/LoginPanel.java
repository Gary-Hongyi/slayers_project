import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

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
    private JLabel loginNoticeLabel;
    // Register
    private FloatInput regNameField, regIdField, regPassField, regWorkField, regHomeField;
    private AvatarPreview regAvatarPreview;
    private String regAvatarPath = "";
    // Reset password
    private FloatInput resetIdField, resetNameField, resetWorkField, resetHomeField;
    private FloatInput resetPassField, resetConfirmField;
    // Success
    private AvatarPreview successAvatarPreview;
    private JLabel successIdLabel;

    private static final String LOGIN = "LOGIN", REGISTER = "REGISTER";
    private static final String RESET = "RESET", SUCCESS = "SUCCESS";

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
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*._-]{6,20}$");
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
        internalPanel.add(buildResetPassword(), RESET);
        internalPanel.add(buildSuccess(), SUCCESS);
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
        form.add(Box.createVerticalStrut(12));

        loginNoticeLabel = centeredLabel(" ", new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.PLAIN, 12), BRAND);
        form.add(loginNoticeLabel);
        form.add(Box.createVerticalStrut(20));

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
        JLabel forgotLink = linkLabel("Forgot password?", 12);
        forgotLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                loginNoticeLabel.setText(" ");
                clearReset();
                internalCards.show(internalPanel, RESET);
            }
        });
        forgotRow.add(forgotLink);
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
                loginNoticeLabel.setText(" ");
                clearReg();
                internalCards.show(internalPanel, REGISTER);
            }
        });
        regRow.add(noAcc); regRow.add(regLink);
        form.add(regRow);

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
        form.add(Box.createVerticalStrut(24));

        regAvatarPreview = new AvatarPreview(64);
        regAvatarPreview.setAlignmentX(Component.CENTER_ALIGNMENT);
        regAvatarPreview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        regAvatarPreview.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { chooseRegisterAvatar(); }
        });
        form.add(regAvatarPreview);
        form.add(Box.createVerticalStrut(8));

        JLabel avatarLink = linkLabel("Choose avatar", 12);
        avatarLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { chooseRegisterAvatar(); }
        });
        form.add(avatarLink);
        form.add(Box.createVerticalStrut(20));

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
    //  RESET PASSWORD SCREEN
    // ================================================================
    private JPanel buildResetPassword() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        form.add(centeredLabel("SnapTok",
                new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 32), BRAND));
        form.add(Box.createVerticalStrut(8));
        form.add(centeredLabel("Reset your password", YH, TEXT_HINT));
        form.add(Box.createVerticalStrut(28));

        resetIdField = new FloatInput("User ID", false);
        form.add(resetIdField);
        form.add(Box.createVerticalStrut(14));

        resetNameField = new FloatInput("Name", false);
        form.add(resetNameField);
        form.add(Box.createVerticalStrut(14));

        resetWorkField = new FloatInput("Workplace", false);
        form.add(resetWorkField);
        form.add(Box.createVerticalStrut(14));

        resetHomeField = new FloatInput("Hometown", false);
        form.add(resetHomeField);
        form.add(Box.createVerticalStrut(14));

        resetPassField = new FloatInput("New password", true);
        form.add(resetPassField);
        form.add(Box.createVerticalStrut(14));

        resetConfirmField = new FloatInput("Confirm password", true);
        form.add(resetConfirmField);
        form.add(Box.createVerticalStrut(28));

        PrimaryButton resetBtn = new PrimaryButton("Reset password");
        resetBtn.addActionListener(e -> doResetPassword());
        form.add(resetBtn);
        form.add(Box.createVerticalStrut(24));

        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        backRow.setOpaque(false);
        JLabel remember = new JLabel("Remember your password?  ");
        remember.setFont(YH); remember.setForeground(TEXT_SUB);
        JLabel backLink = linkLabel("Sign in", 14);
        backLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { internalCards.show(internalPanel, LOGIN); }
        });
        backRow.add(remember); backRow.add(backLink);
        form.add(backRow);

        JScrollPane sp = new JScrollPane(form);
        sp.setBorder(null); sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        wrap.add(sp);
        return wrap;
    }

    // ================================================================
    //  REGISTER SUCCESS SCREEN
    // ================================================================
    private JPanel buildSuccess() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        form.add(centeredLabel("SnapTok",
                new Font("\u5fae\u8f6f\u96c5\u9ed1", Font.BOLD, 32), BRAND));
        form.add(Box.createVerticalStrut(8));
        form.add(centeredLabel("Account created successfully", YH, TEXT_HINT));
        form.add(Box.createVerticalStrut(28));

        successAvatarPreview = new AvatarPreview(72);
        successAvatarPreview.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(successAvatarPreview);
        form.add(Box.createVerticalStrut(14));

        successIdLabel = centeredLabel("@", YH_BOLD, TEXT_MAIN);
        form.add(successIdLabel);
        form.add(Box.createVerticalStrut(32));

        PrimaryButton signIn = new PrimaryButton("Sign in now");
        signIn.addActionListener(e -> internalCards.show(internalPanel, LOGIN));
        form.add(signIn);

        wrap.add(form);
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
        if (!isValidUserId(id)) { err(userIdRuleMessage()); return; }

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
        if (!isValidUserId(id)) { err(userIdRuleMessage()); return; }
        if (pw.isEmpty()) { err("Please create a password."); return; }
        if (!isValidPassword(pw)) { err(passwordRuleMessage()); return; }
        if (hasUnsafeUserFileChars(name) || hasUnsafeUserFileChars(work) || hasUnsafeUserFileChars(home)) {
            err("Name, workplace, and hometown cannot contain commas or vertical bars.");
            return;
        }

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
        newUser.setAvatarPath(regAvatarPath);
        network.addUser(newUser);

        // Persist to users.txt
        saveUserToFile(id, pw, name, workplace, hometown);

        showRegisterSuccess(id, regAvatarPath);
        clearReg();
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

    private void doResetPassword() {
        loadUsersFile();

        String id = resetIdField.getText().trim();
        String name = resetNameField.getText().trim();
        String work = resetWorkField.getText().trim();
        String home = resetHomeField.getText().trim();
        String newPassword = resetPassField.getText();
        String confirmPassword = resetConfirmField.getText();

        if (id.isEmpty() || name.isEmpty() || work.isEmpty() || home.isEmpty()) {
            err("Please fill in User ID, name, workplace, and hometown.");
            return;
        }
        if (!isValidUserId(id)) { err(userIdRuleMessage()); return; }

        User user = network.getUser(id);
        if (user == null) {
            loadUsersFile();
            user = network.getUser(id);
        }
        if (user == null) {
            err("No account was found for that User ID.");
            return;
        }
        if (!sameText(user.getName(), name)
                || !sameText(user.getWorkplace(), work)
                || !sameText(user.getHometown(), home)) {
            err("The profile details do not match this account.");
            return;
        }
        if (!isValidPassword(newPassword)) { err(passwordRuleMessage()); return; }
        if (!newPassword.equals(confirmPassword)) { err("The two passwords do not match."); return; }

        user.setPassword(newPassword);
        rewriteUsersFile(network);
        loginIdField.setText(id);
        loginPassField.setText("");
        clearReset();
        loginNoticeLabel.setText("Password reset successfully. Please sign in.");
        internalCards.show(internalPanel, LOGIN);
    }

    private void clearReg() {
        regNameField.setText(""); regIdField.setText("");
        regPassField.setText(""); regWorkField.setText("");
        regHomeField.setText("");
        regAvatarPath = "";
        if (regAvatarPreview != null) regAvatarPreview.setImagePath("");
    }

    private void clearReset() {
        resetIdField.setText(""); resetNameField.setText("");
        resetWorkField.setText(""); resetHomeField.setText("");
        resetPassField.setText(""); resetConfirmField.setText("");
    }

    private void chooseRegisterAvatar() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            regAvatarPath = fc.getSelectedFile().getAbsolutePath();
            regAvatarPreview.setImagePath(regAvatarPath);
        }
    }

    private void showRegisterSuccess(String userId, String avatarPath) {
        successIdLabel.setText("@" + userId);
        successAvatarPreview.setImagePath(avatarPath);
        loginIdField.setText(userId);
        loginPassField.setText("");
        internalCards.show(internalPanel, SUCCESS);
    }

    private void err(String msg) {
        JOptionPane.showMessageDialog(this, msg, "SnapTok", JOptionPane.WARNING_MESSAGE);
    }

    private static boolean isValidUserId(String id) {
        return id != null && USER_ID_PATTERN.matcher(id).matches();
    }

    private static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    private static boolean hasUnsafeUserFileChars(String value) {
        return value != null && (value.contains(",") || value.contains("|"));
    }

    private static boolean sameText(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private static String userIdRuleMessage() {
        return "User ID must be 3-16 characters and may contain letters, numbers, and underscores only.";
    }

    private static String passwordRuleMessage() {
        return "Password must be 6-20 characters, include at least one letter and one number, and may use !@#$%^&*._-.";
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
                String[] parts = splitUserRecord(line);
                if (parts.length >= 5) {
                    String uid = unescapeUserField(parts[0]).trim();
                    String pw = unescapeUserField(parts[1]).trim();
                    String name = unescapeUserField(parts[2]).trim();
                    String work = unescapeUserField(parts[3]).trim();
                    String home = unescapeUserField(parts[4]).trim();
                    if (network.getUser(uid) == null) {
                        User newUser = new User(uid, name, work, home, pw);
                        if (parts.length >= 6) newUser.setSignature(unescapeUserField(parts[5]).trim());
                        if (parts.length >= 7) newUser.setAvatarPath(unescapeUserField(parts[6]).trim());
                        if (parts.length >= 8) parseRemarks(unescapeUserField(parts[7]), newUser);
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
                pw.println(escapeUserField(u.getUserId()) + ","
                        + escapeUserField(u.getPassword()) + ","
                        + escapeUserField(u.getName()) + ","
                        + escapeUserField(u.getWorkplace()) + ","
                        + escapeUserField(u.getHometown()) + ","
                        + escapeUserField(u.getSignature()) + ","
                        + escapeUserField(u.getAvatarPath()) + ","
                        + escapeUserField(serializeRemarks(u)));
            }
        } catch (IOException e) {
            /* silently ignore write errors */
        }
    }

    private static String[] splitUserRecord(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (escaping) {
                current.append('\\').append(ch);
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else if (ch == ',') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (escaping) current.append('\\');
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private static String escapeUserField(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace(",", "\\,");
    }

    private static String unescapeUserField(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaping) {
                out.append(ch);
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else {
                out.append(ch);
            }
        }
        if (escaping) out.append('\\');
        return out.toString();
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
            user.setFriendRemark(entry.substring(0, sep), decode(entry.substring(sep + 1)));
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

    static class AvatarPreview extends JPanel {
        private final int size;
        private String imagePath = "";
        private BufferedImage image;

        AvatarPreview(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setOpaque(false);
        }

        void setImagePath(String path) {
            imagePath = path != null ? path : "";
            image = null;
            if (!imagePath.isEmpty()) {
                try {
                    image = ImageIO.read(new File(imagePath));
                } catch (IOException ignored) {
                    image = null;
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int d = Math.min(size, Math.min(getWidth(), getHeight()));
            int x = (getWidth() - d) / 2;
            int y = (getHeight() - d) / 2;
            Ellipse2D.Double clip = new Ellipse2D.Double(x, y, d, d);

            if (image != null) {
                Shape oldClip = g2.getClip();
                g2.setClip(clip);
                double scale = Math.max(d / (double) image.getWidth(), d / (double) image.getHeight());
                int w = (int) Math.ceil(image.getWidth() * scale);
                int h = (int) Math.ceil(image.getHeight() * scale);
                g2.drawImage(image, x + (d - w) / 2, y + (d - h) / 2, w, h, null);
                g2.setClip(oldClip);
            } else {
                g2.setColor(INPUT_BG);
                g2.fill(clip);
                g2.setColor(TEXT_HINT);
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = x + d / 2;
                int cy = y + d / 2;
                g2.drawOval(cx - d / 8, cy - d / 4, d / 4, d / 4);
                g2.drawArc(cx - d / 4, cy, d / 2, d / 3, 0, 180);
            }

            g2.setColor(new Color(226, 232, 240));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(clip);
            g2.dispose();
        }
    }
}
