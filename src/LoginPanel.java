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
 * Apple-style login panel with floating label inputs and clean layout.
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
    private JPanel resetPasswordPanel;
    private JLabel resetVerifiedLabel;
    private User resetVerifiedUser;
    // Success
    private AvatarPreview successAvatarPreview;
    private JLabel successIdLabel;

    private static final String LOGIN = "LOGIN", REGISTER = "REGISTER";
    private static final String RESET = "RESET", SUCCESS = "SUCCESS";

    // Apple Design System — clean, minimal, Action Blue accent
    private static final String FONT_FAMILY = "Segoe UI";
    private static final Color BRAND = new Color(0, 102, 204);         // #0066cc Action Blue
    private static final Color BRAND_DARK = new Color(0, 89, 178);     // press state
    private static final Color TEXT_MAIN = new Color(29, 29, 31);      // #1d1d1f ink
    private static final Color TEXT_SUB = new Color(122, 122, 122);    // #7a7a7a muted-48
    private static final Color TEXT_HINT = new Color(122, 122, 122);   // #7a7a7a muted-48
    private static final Color INPUT_BG = new Color(255, 255, 255);    // #ffffff white input background
    private static final Color HAIRLINE = new Color(224, 224, 224);    // #e0e0e0 hairline
    private static final int FIELD_W = 352;
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
                new Font(FONT_FAMILY, Font.BOLD, 34), BRAND);
        form.add(title);
        form.add(Box.createVerticalStrut(6));

        // Subtitle
        form.add(centeredLabel("Sign in to your account",
                new Font(FONT_FAMILY, Font.PLAIN, 17), TEXT_HINT));
        form.add(Box.createVerticalStrut(10));

        loginNoticeLabel = centeredLabel(" ", new Font(FONT_FAMILY, Font.PLAIN, 12), BRAND);
        form.add(loginNoticeLabel);
        form.add(Box.createVerticalStrut(20));

        // User ID
        loginIdField = new FloatInput("User ID", false);
        form.add(loginIdField);
        form.add(Box.createVerticalStrut(12));

        // Password
        loginPassField = new FloatInput("Password", true);
        form.add(loginPassField);
        form.add(Box.createVerticalStrut(10));

        // Forgot password
        JPanel forgotRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        forgotRow.setOpaque(false);
        forgotRow.setMaximumSize(new Dimension(FIELD_W, 20));
        forgotRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel forgotLink = linkLabel("Forgot password?", 14);
        forgotLink.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                loginNoticeLabel.setText(" ");
                clearReset();
                internalCards.show(internalPanel, RESET);
            }
        });
        forgotRow.add(forgotLink);
        form.add(forgotRow);
        form.add(Box.createVerticalStrut(20));

        // Sign in button
        PrimaryButton signIn = new PrimaryButton("Sign in");
        signIn.addActionListener(e -> doLogin());
        form.add(signIn);
        form.add(Box.createVerticalStrut(16));

        // Separator
        form.add(buildSeparator("OR"));
        form.add(Box.createVerticalStrut(16));

        // Register link
        JPanel regRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        regRow.setOpaque(false);
        JLabel noAcc = new JLabel("Don't have an account?  ");
        noAcc.setFont(new Font(FONT_FAMILY, Font.PLAIN, 17)); noAcc.setForeground(TEXT_SUB);
        JLabel regLink = linkLabel("Create account", 17);
        regLink.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                loginNoticeLabel.setText(" ");
                clearReg();
                internalCards.show(internalPanel, REGISTER);
            }
        });
        regRow.add(noAcc); regRow.add(regLink);
        form.add(regRow);

        JPanel card = createCardPanel(56, 64, 48);
        card.add(form, BorderLayout.CENTER);
        wrap.add(card);
        return wrap;
    }

    // ================================================================
    //  REGISTER SCREEN
    // ================================================================
    private JPanel buildRegister() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        // Inner form — uses BoxLayout, no fixed max height so JScrollPane can scroll it
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(36, 64, 40, 64));

        form.add(centeredLabel("SnapTok",
                new Font(FONT_FAMILY, Font.BOLD, 34), BRAND));
        form.add(Box.createVerticalStrut(6));
        form.add(centeredLabel("Create your account",
                new Font(FONT_FAMILY, Font.PLAIN, 17), TEXT_HINT));
        form.add(Box.createVerticalStrut(24));

        regAvatarPreview = new AvatarPreview(72);
        regAvatarPreview.setAlignmentX(Component.CENTER_ALIGNMENT);
        regAvatarPreview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        regAvatarPreview.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { chooseRegisterAvatar(); }
        });
        form.add(regAvatarPreview);
        form.add(Box.createVerticalStrut(8));

        JLabel avatarLink = linkLabel("Choose avatar", 14);
        avatarLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarLink.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { chooseRegisterAvatar(); }
        });
        form.add(avatarLink);
        form.add(Box.createVerticalStrut(20));

        regNameField = new FloatInput("Name", false);
        form.add(regNameField);
        form.add(Box.createVerticalStrut(10));

        regIdField = new FloatInput("Choose a User ID", false);
        form.add(regIdField);
        form.add(Box.createVerticalStrut(10));

        regPassField = new FloatInput("Password", true);
        form.add(regPassField);
        form.add(Box.createVerticalStrut(10));

        regWorkField = new FloatInput("Workplace", false);
        form.add(regWorkField);
        form.add(Box.createVerticalStrut(10));

        regHomeField = new FloatInput("Hometown", false);
        form.add(regHomeField);
        form.add(Box.createVerticalStrut(20));

        PrimaryButton createBtn = new PrimaryButton("Create account");
        createBtn.addActionListener(e -> doRegister());
        form.add(createBtn);
        form.add(Box.createVerticalStrut(16));

        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        backRow.setOpaque(false);
        JLabel already = new JLabel("Already have an account?  ");
        already.setFont(new Font(FONT_FAMILY, Font.PLAIN, 17)); already.setForeground(TEXT_SUB);
        JLabel backLink = linkLabel("Sign in", 17);
        backLink.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                internalCards.show(internalPanel, LOGIN);
            }
        });
        backRow.add(already); backRow.add(backLink);
        form.add(backRow);
        form.add(Box.createVerticalStrut(8));

        // Scroll pane wraps the form so nothing gets clipped at any window height
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        // White card that fills the scroll pane
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(0, 2, w, h, 12, 12);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h - 2, 12, 12);
                g2.setColor(HAIRLINE);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 2, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(FIELD_W + 128, 560));
        card.add(scroll, BorderLayout.CENTER);

        wrap.add(card);
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
                new Font(FONT_FAMILY, Font.BOLD, 34), BRAND));
        form.add(Box.createVerticalStrut(8));
        form.add(centeredLabel("Reset your password",
                new Font(FONT_FAMILY, Font.PLAIN, 17), TEXT_HINT));
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

        PrimaryButton verifyBtn = new PrimaryButton("Verify identity");
        verifyBtn.addActionListener(e -> verifyResetIdentity());
        form.add(verifyBtn);
        form.add(Box.createVerticalStrut(18));

        resetPasswordPanel = new JPanel();
        resetPasswordPanel.setLayout(new BoxLayout(resetPasswordPanel, BoxLayout.Y_AXIS));
        resetPasswordPanel.setOpaque(false);
        resetPasswordPanel.setVisible(false);

        resetVerifiedLabel = centeredLabel("Identity verified. Set a new password.", 
                new Font(FONT_FAMILY, Font.PLAIN, 14), BRAND);
        resetPasswordPanel.add(resetVerifiedLabel);
        resetPasswordPanel.add(Box.createVerticalStrut(14));

        resetPassField = new FloatInput("New password", true);
        resetPasswordPanel.add(resetPassField);
        resetPasswordPanel.add(Box.createVerticalStrut(14));

        resetConfirmField = new FloatInput("Confirm password", true);
        resetPasswordPanel.add(resetConfirmField);
        resetPasswordPanel.add(Box.createVerticalStrut(28));

        PrimaryButton resetBtn = new PrimaryButton("Save new password");
        resetBtn.addActionListener(e -> doResetPassword());
        resetPasswordPanel.add(resetBtn);
        form.add(resetPasswordPanel);
        form.add(Box.createVerticalStrut(24));

        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        backRow.setOpaque(false);
        JLabel remember = new JLabel("Remember your password?  ");
        remember.setFont(new Font(FONT_FAMILY, Font.PLAIN, 17)); remember.setForeground(TEXT_SUB);
        JLabel backLink = linkLabel("Sign in", 17);
        backLink.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { internalCards.show(internalPanel, LOGIN); }
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
                new Font(FONT_FAMILY, Font.BOLD, 34), BRAND));
        form.add(Box.createVerticalStrut(8));
        form.add(centeredLabel("Account created successfully",
                new Font(FONT_FAMILY, Font.PLAIN, 17), TEXT_HINT));
        form.add(Box.createVerticalStrut(28));

        successAvatarPreview = new AvatarPreview(72);
        successAvatarPreview.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(successAvatarPreview);
        form.add(Box.createVerticalStrut(14));

        successIdLabel = centeredLabel("@", 
                new Font(FONT_FAMILY, Font.BOLD, 14), TEXT_MAIN);
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
            showStyledDialog("Invalid User ID or Password");
            return;
        }
        if (!u.getPassword().equals(pw)) {
            showStyledDialog("Invalid User ID or Password");
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
        if (!isValidPassword(pw)) { showStyledDialog(passwordRuleMessage()); return; }
        if (hasUnsafeUserFileChars(name) || hasUnsafeUserFileChars(work) || hasUnsafeUserFileChars(home)) {
            err("Name, workplace, and hometown cannot contain commas or vertical bars.");
            return;
        }

        // Check both in-memory and file
        loadUsersFile();
        if (network.getUser(id) != null) {
            showStyledDialog("User ID already exists. Please choose a different one.");
            return;
        }

        String workplace = work.isEmpty() ? "Unknown" : work;
        String hometown = home.isEmpty() ? "Unknown" : home;

        String avatarPath = copyAvatarToAssets(regAvatarPath);
        User newUser = new User(id, name, workplace, hometown, pw);
        newUser.setAvatarPath(avatarPath);
        network.addUser(newUser);

        // Persist to users.txt
        saveUserToFile(id, pw, name, workplace, hometown);

        showRegisterSuccess(id, avatarPath);
        clearReg();
    }


    private void verifyResetIdentity() {
        loadUsersFile();

        String id = resetIdField.getText().trim();
        String name = resetNameField.getText().trim();
        String work = resetWorkField.getText().trim();
        String home = resetHomeField.getText().trim();

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

        resetVerifiedUser = user;
        setResetIdentityEnabled(false);
        resetVerifiedLabel.setText("Verified @" + id + ". Set a new password.");
        resetPasswordPanel.setVisible(true);
        resetPasswordPanel.revalidate();
        resetPasswordPanel.repaint();
        SwingUtilities.invokeLater(resetPassField::requestFocusInWindow);
    }

    private void doResetPassword() {
        if (resetVerifiedUser == null) {
            verifyResetIdentity();
            if (resetVerifiedUser == null) return;
        }

        String newPassword = resetPassField.getText();
        String confirmPassword = resetConfirmField.getText();
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            err("Please enter and confirm your new password.");
            return;
        }
        if (!isValidPassword(newPassword)) { showStyledDialog(passwordRuleMessage()); return; }
        if (!newPassword.equals(confirmPassword)) { err("The two passwords do not match."); return; }

        resetVerifiedUser.setPassword(newPassword);
        rewriteUsersFile(network);
        loginIdField.setText(resetVerifiedUser.getUserId());
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
        resetVerifiedUser = null;
        setResetIdentityEnabled(true);
        if (resetPasswordPanel != null) resetPasswordPanel.setVisible(false);
    }

    private void setResetIdentityEnabled(boolean enabled) {
        if (resetIdField != null) resetIdField.setEnabled(enabled);
        if (resetNameField != null) resetNameField.setEnabled(enabled);
        if (resetWorkField != null) resetWorkField.setEnabled(enabled);
        if (resetHomeField != null) resetHomeField.setEnabled(enabled);
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
        showStyledDialog(msg);
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
        return "<html><div style='text-align:center; width:360px;'>"
                + "Use 6-20 chars with letters and numbers.<br>"
                + "Allowed symbols: ! @ # $ % ^ &amp; * . _ -"
                + "</div></html>";
    }

    /** Styled message dialog — Apple design, replaces JOptionPane */
    void showStyledDialog(String message) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "SnapTok", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(0, 2, getWidth(), getHeight(), 16, 16);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() - 2, 16, 16);
                g2.setColor(HAIRLINE);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 16, 16);
                g2.dispose();
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));

        JLabel msg = new JLabel("<html><div style='text-align:center;width:280px;'>" +
                message.replace("\n", "<br>") + "</div></html>");
        msg.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        msg.setForeground(TEXT_MAIN);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(msg);
        panel.add(Box.createVerticalStrut(22));

        JButton ok = new JButton("OK") {
            private boolean hov;
            { setFont(new Font(FONT_FAMILY, Font.BOLD, 15)); setForeground(Color.WHITE);
              setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
              setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
              setPreferredSize(new Dimension(160, 42));
              setMaximumSize(new Dimension(160, 42));
              addMouseListener(new MouseAdapter() {
                  public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                  public void mouseExited(MouseEvent e) { hov = false; repaint(); }
              });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? BRAND_DARK : BRAND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose(); super.paintComponent(g);
            }
        };
        ok.addActionListener(e -> dialog.dispose());
        ok.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(ok);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.setVisible(true);
    }

    static File getProjectRoot() {
        File cwd = new File(System.getProperty("user.dir")).getAbsoluteFile();
        if ("src".equalsIgnoreCase(cwd.getName()) && cwd.getParentFile() != null) {
            return cwd.getParentFile();
        }
        File nested = new File(cwd, "slayers_project");
        if (nested.isDirectory()) return nested;
        return cwd;
    }

    static String normalizeAvatarPath(String avatarPath) {
        if (avatarPath == null || avatarPath.trim().isEmpty()) return "";
        String clean = avatarPath.trim();
        File file = new File(clean);
        if (!file.isAbsolute()) {
            return clean.replace('\\', '/');
        }
        return copyAvatarToAssets(clean);
    }

    /** Copies an avatar image to the project's assets/avatars directory and returns the relative path. */
    static String copyAvatarToAssets(String sourcePath) {
        if (sourcePath == null || sourcePath.isEmpty()) return "";
        try {
            Path source = Paths.get(sourcePath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(source)) return "";

            Path root = getProjectRoot().toPath().toAbsolutePath().normalize();
            Path avatarDir = root.resolve("assets").resolve("avatars");
            Files.createDirectories(avatarDir);

            if (source.startsWith(avatarDir)) {
                return root.relativize(source).toString().replace(File.separatorChar, '/');
            }

            String originalName = source.getFileName().toString();
            String safeName = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
            String fileName = System.currentTimeMillis() + "_" + safeName;
            Path dest = avatarDir.resolve(fileName);
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            return root.relativize(dest).toString().replace(File.separatorChar, '/');
        } catch (Exception e) {
            return "";
        }
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
        boolean migratedAvatarPath = false;
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
                    String avatarPath = "";
                    if (parts.length >= 7) {
                        String rawAvatarPath = unescapeUserField(parts[6]).trim();
                        avatarPath = normalizeAvatarPath(rawAvatarPath);
                        if (!avatarPath.equals(rawAvatarPath)) migratedAvatarPath = true;
                    }
                    if (network.getUser(uid) == null) {
                        User newUser = new User(uid, name, work, home, pw);
                        if (parts.length >= 6) newUser.setSignature(unescapeUserField(parts[5]).trim());
                        newUser.setAvatarPath(avatarPath);
                        if (parts.length >= 8) parseRemarks(unescapeUserField(parts[7]), newUser);
                        network.addUser(newUser);
                    } else if (!avatarPath.isEmpty()) {
                        network.getUser(uid).setAvatarPath(avatarPath);
                    }
                }
            }
        } catch (IOException e) {
            /* silently ignore read errors */
        }
        if (migratedAvatarPath) rewriteUsersFile(network);
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
        l.setFont(new Font(FONT_FAMILY, Font.PLAIN, size));
        l.setForeground(BRAND);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { l.setForeground(BRAND_DARK); }
            public void mouseExited(MouseEvent e) { l.setForeground(BRAND); }
        });
        return l;
    }

    private JPanel buildSeparator(String text) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(FIELD_W, 20));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;

        // Left line — 0.5px hairline
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel leftLine = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(HAIRLINE);
                g2.setStroke(new BasicStroke(0.5f));
                int y = getHeight() / 2;
                g2.drawLine(0, y, getWidth(), y);
                g2.dispose();
            }
        };
        leftLine.setOpaque(false);
        row.add(leftLine, gbc);

        // Text — 13px, ink-48 color
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 12, 0, 12);
        JLabel t = new JLabel(text, SwingConstants.CENTER);
        t.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        t.setForeground(TEXT_HINT);
        row.add(t, gbc);

        // Right line — 0.5px hairline
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel rightLine = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(HAIRLINE);
                g2.setStroke(new BasicStroke(0.5f));
                int y = getHeight() / 2;
                g2.drawLine(0, y, getWidth(), y);
                g2.dispose();
            }
        };
        rightLine.setOpaque(false);
        row.add(rightLine, gbc);

        return row;
    }

    // ================================================================
    //  WHITE CARD — rounded 12px, hairline border, shadow
    // ================================================================
    private JPanel createCardPanel(int topPad, int sidePad, int bottomPad) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                // Shadow
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(0, 2, w, h, 12, 12);
                // White fill
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h - 2, 12, 12);
                // Hairline border
                g2.setColor(HAIRLINE);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 2, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(topPad, sidePad, bottomPad, sidePad));
        return card;
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

            field.setFont(new Font(FONT_FAMILY, Font.PLAIN, 17));
            field.setForeground(TEXT_MAIN);
            field.setCaretColor(BRAND);
            field.setOpaque(false);
            field.setBorder(BorderFactory.createEmptyBorder(18, 16, 6, password ? 44 : 16));
            field.setBounds(0, 0, FIELD_W, 50);
            field.setBackground(INPUT_BG);

            // Overlay label
            label = new JLabel(labelText);
            label.setFont(new Font(FONT_FAMILY, Font.PLAIN, 17));
            label.setForeground(TEXT_HINT);
            label.setBounds(16, 14, FIELD_W - 60, 22);

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

            // White background — matches HTML mockup input-field
            g2.setColor(INPUT_BG); // #ffffff
            g2.fillRoundRect(0, 0, getWidth(), 50, 10, 10);

            // Subtle gray border like HTML - 1px #d1d1d6
            g2.setColor(new Color(209, 209, 214));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, 49, 10, 10);

            // Focus: soft blue glow ring (3px, 25% opacity)
            if (focused) {
                g2.setColor(new Color(0, 102, 204, 64)); // rgba(0,102,204,0.25)
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(1, 1, getWidth() - 2, 48, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }

        private void animateLabel(boolean up) {
            Timer timer = new Timer(10, null);
            final int[] step = {0};
            final int startY = label.getY();
            final int targetY = up ? 4 : 14;
            timer.addActionListener(e -> {
                step[0]++;
                float p = Math.min(step[0] / 12f, 1f);
                float ease = 1 - (1 - p) * (1 - p);
                label.setLocation(label.getX(), (int) (startY + (targetY - startY) * ease));
                float sz = up ? 17f + (13f - 17f) * ease : 13f + (17f - 13f) * ease;
                label.setFont(new Font(FONT_FAMILY, Font.PLAIN, Math.max(13, (int) sz)));
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

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (field != null) field.setEnabled(enabled);
            repaint();
        }
    }

    // ================================================================
    //  PRIMARY BUTTON — Apple flat pill, Action Blue, press darkens
    // ================================================================

    static class PrimaryButton extends JButton {
        private boolean hover;
        private boolean pressed;

        PrimaryButton(String text) {
            super(text);
            setFont(new Font(FONT_FAMILY, Font.PLAIN, 17));
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
            // Apple: flat fill, darker on press - 8px rounded rectangle
            g2.setColor(pressed ? BRAND_DARK : BRAND);
            int arc = 8; // 8px corner radius for rounded rectangle
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
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
                    File file = new File(imagePath);
                    if (!file.isAbsolute()) {
                        file = new File(getProjectRoot(), imagePath);
                    }
                    image = ImageIO.read(file);
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

            g2.setColor(HAIRLINE); // hairline border
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(clip);
            g2.dispose();
        }
    }
}
