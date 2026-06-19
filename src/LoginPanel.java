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
 * Provides the login, registration and password reset screens for SnapTok.
 *
 * @author Team Slayers
 * @version 1.0
 */
public class LoginPanel extends JPanel {

    private MainGUI mainGUI;
    private SocialNetwork network;
    private CardLayout internalCards;
    private JPanel internalPanel;

    private FloatInput loginIdField;
    private FloatInput loginPassField;
    private JLabel loginNoticeLabel;

    private FloatInput regNameField, regIdField, regPassField, regWorkField, regHomeField;
    private AvatarPreview regAvatarPreview;
    private String regAvatarPath = "";

    private FloatInput resetIdField, resetNameField, resetWorkField, resetHomeField;
    private FloatInput resetPassField, resetConfirmField;
    private JPanel resetPasswordPanel;
    private JLabel resetVerifiedLabel;
    private User resetVerifiedUser;

    private AvatarPreview successAvatarPreview;
    private JLabel successIdLabel;

    private static final String LOGIN = "LOGIN", REGISTER = "REGISTER";
    private static final String RESET = "RESET", SUCCESS = "SUCCESS";

    private static final String FONT_FAMILY = "Segoe UI";
    private static final Color BRAND = new Color(0, 102, 204);
    private static final Color BRAND_DARK = new Color(0, 89, 178);
    private static final Color TEXT_MAIN = new Color(29, 29, 31);
    private static final Color TEXT_SUB = new Color(122, 122, 122);
    private static final Color TEXT_HINT = new Color(122, 122, 122);
    private static final Color INPUT_BG = new Color(255, 255, 255);
    private static final Color HAIRLINE = new Color(224, 224, 224);
    private static final int FIELD_W = 352;
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*._-]{6,20}$");
    private static final Pattern PASSWORD_LETTER_PATTERN = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern PASSWORD_NUMBER_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern PASSWORD_ALLOWED_CHARS_PATTERN =
            Pattern.compile("[A-Za-z\\d!@#$%^&*._-]*");
    static final String USERS_FILE;
    static {
        USERS_FILE = new File(getProjectRoot(), "users.txt").getAbsolutePath();
    }

    /**
     * Constructs a new LoginPanel object.
     */
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

        loadUsersFile();
    }

    /**
     * Builds the login.
     */
    private JPanel buildLogin() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JLabel title = centeredLabel("SnapTok",
                new Font(FONT_FAMILY, Font.BOLD, 34), BRAND);
        form.add(title);
        form.add(Box.createVerticalStrut(6));

        form.add(centeredLabel("Sign in to your account",
                new Font(FONT_FAMILY, Font.PLAIN, 17), TEXT_HINT));
        form.add(Box.createVerticalStrut(10));

        loginNoticeLabel = centeredLabel(" ", new Font(FONT_FAMILY, Font.PLAIN, 12), BRAND);
        form.add(loginNoticeLabel);
        form.add(Box.createVerticalStrut(20));

        loginIdField = new FloatInput("User ID", false);
        form.add(loginIdField);
        form.add(Box.createVerticalStrut(12));

        loginPassField = new FloatInput("Password", true);
        form.add(loginPassField);
        form.add(Box.createVerticalStrut(10));

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

        PrimaryButton signIn = new PrimaryButton("Sign in");
        signIn.addActionListener(e -> doLogin());
        form.add(signIn);
        form.add(Box.createVerticalStrut(16));

        form.add(buildSeparator("OR"));
        form.add(Box.createVerticalStrut(16));

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

    /**
     * Builds the register.
     */
    private JPanel buildRegister() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        JPanel intro = new JPanel();
        intro.setLayout(new BoxLayout(intro, BoxLayout.Y_AXIS));
        intro.setOpaque(false);

        intro.add(centeredLabel("SnapTok",
                new Font(FONT_FAMILY, Font.BOLD, 34), BRAND));
        intro.add(Box.createVerticalStrut(6));
        intro.add(centeredLabel("Create your account",
                new Font(FONT_FAMILY, Font.PLAIN, 17), TEXT_HINT));
        intro.add(Box.createVerticalStrut(28));

        regAvatarPreview = new AvatarPreview(72);
        regAvatarPreview.setAlignmentX(Component.CENTER_ALIGNMENT);
        regAvatarPreview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        regAvatarPreview.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { chooseRegisterAvatar(); }
        });
        intro.add(regAvatarPreview);
        intro.add(Box.createVerticalStrut(8));

        JLabel avatarLink = linkLabel("Choose avatar", 14);
        avatarLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarLink.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { chooseRegisterAvatar(); }
        });
        intro.add(avatarLink);
        intro.add(Box.createVerticalStrut(30));

        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setOpaque(false);

        regNameField = new FloatInput("Name", false);
        fields.add(regNameField);
        fields.add(Box.createVerticalStrut(10));

        regIdField = new FloatInput("Choose a User ID", false);
        fields.add(regIdField);
        fields.add(Box.createVerticalStrut(10));

        regPassField = new FloatInput("Password", true);
        fields.add(regPassField);
        fields.add(Box.createVerticalStrut(10));

        regWorkField = new FloatInput("Workplace", false);
        fields.add(regWorkField);
        fields.add(Box.createVerticalStrut(10));

        regHomeField = new FloatInput("Hometown", false);
        fields.add(regHomeField);
        fields.add(Box.createVerticalStrut(18));

        PrimaryButton createBtn = new PrimaryButton("Create account");
        createBtn.addActionListener(e -> doRegister());
        fields.add(createBtn);

        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        backRow.setOpaque(false);
        JLabel already = new JLabel("Already have an account?  ");
        already.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15)); already.setForeground(TEXT_SUB);
        JLabel backLink = linkLabel("Sign in", 15);
        backLink.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                internalCards.show(internalPanel, LOGIN);
            }
        });
        backRow.add(already); backRow.add(backLink);
        backRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        intro.add(backRow);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 0, 44);
        form.add(intro, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(fields, gbc);

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
        card.setBorder(BorderFactory.createEmptyBorder(40, 48, 40, 48));
        card.setPreferredSize(new Dimension(FIELD_W + 380, 500));
        card.add(form, BorderLayout.CENTER);

        wrap.add(card);
        return wrap;
    }

    /**
     * Builds the reset password.
     */
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

    /**
     * Builds the success.
     */
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

        successIdLabel = centeredLabel("ID:",
                new Font(FONT_FAMILY, Font.BOLD, 14), TEXT_MAIN);
        form.add(successIdLabel);
        form.add(Box.createVerticalStrut(32));

        PrimaryButton signIn = new PrimaryButton("Sign in now");
        signIn.addActionListener(e -> internalCards.show(internalPanel, LOGIN));
        form.add(signIn);

        wrap.add(form);
        return wrap;
    }

    /**
     * Validates the login form and signs in the user when the credentials are correct.
     */
    private void doLogin() {
        String id = loginIdField.getText().trim();
        String pw = loginPassField.getText();
        if (id.isEmpty()) { err("Please enter your User ID."); return; }
        if (pw.isEmpty()) { err("Please enter your password."); return; }
        if (!isValidUserId(id)) { err(userIdRuleMessage()); return; }

        User u = network.getUser(id);
        if (u == null) {

            loadUsersFile();
            u = network.getUser(id);
        }
        if (u == null) {
            showStyledDialog("Please check your User ID or password.");
            return;
        }
        if (!u.getPassword().equals(pw)) {
            showStyledDialog("Please check your User ID or password.");
            loginPassField.setText("");
            return;
        }
        network.setCurrentUser(u);
        loginPassField.setText("");
        mainGUI.showMainContent();
    }

    /**
     * Validates the registration form and creates a new user account.
     */
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
        if (!isValidPassword(pw)) { showStyledDialog(passwordRuleMessage(pw)); return; }
        if (hasUnsafeUserFileChars(name) || hasUnsafeUserFileChars(work) || hasUnsafeUserFileChars(home)) {
            err("Please remove commas or vertical bars from name, workplace, and hometown.");
            return;
        }

        loadUsersFile();
        if (network.getUser(id) != null) {
            showStyledDialog("User ID already exists. Please choose a different one.");
            return;
        }

        String workplace = work.isEmpty() ? "Unknown" : work;
        String hometown = home.isEmpty() ? "Unknown" : home;

        String avatarPath = copyAvatarToImageFolder(regAvatarPath);
        User newUser = new User(id, name, workplace, hometown, pw);
        newUser.setAvatarPath(avatarPath);
        network.addUser(newUser);

        saveUserToFile(id, pw, name, workplace, hometown);

        showRegisterSuccess(id, avatarPath);
        clearReg();
    }

    /**
     * Checks whether the reset-password identity fields match an existing account.
     */
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
            err("Please check the User ID. No account was found.");
            return;
        }
        if (!sameText(user.getName(), name)
                || !sameText(user.getWorkplace(), work)
                || !sameText(user.getHometown(), home)) {
            err("Please check the profile details. They do not match this account.");
            return;
        }

        resetVerifiedUser = user;
        setResetIdentityEnabled(false);
        resetVerifiedLabel.setText("Verified ID: " + id + ". Set a new password.");
        resetPasswordPanel.setVisible(true);
        resetPasswordPanel.revalidate();
        resetPasswordPanel.repaint();
        SwingUtilities.invokeLater(resetPassField::requestFocusInWindow);
    }

    /**
     * Validates and saves a new password for an existing account.
     */
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
        if (!isValidPassword(newPassword)) { showStyledDialog(passwordRuleMessage(newPassword)); return; }
        if (!newPassword.equals(confirmPassword)) { err("Please make sure the two passwords match."); return; }

        resetVerifiedUser.setPassword(newPassword);
        rewriteUsersFile(network);
        loginIdField.setText(resetVerifiedUser.getUserId());
        loginPassField.setText("");
        clearReset();
        loginNoticeLabel.setText("Password reset successfully. Please sign in.");
        internalCards.show(internalPanel, LOGIN);
    }

    /**
     * Clears the reg.
     */
    private void clearReg() {
        regNameField.setText(""); regIdField.setText("");
        regPassField.setText(""); regWorkField.setText("");
        regHomeField.setText("");
        regAvatarPath = "";
        if (regAvatarPreview != null) regAvatarPreview.setImagePath("");
    }

    /**
     * Clears the reset.
     */
    private void clearReset() {
        resetIdField.setText(""); resetNameField.setText("");
        resetWorkField.setText(""); resetHomeField.setText("");
        resetPassField.setText(""); resetConfirmField.setText("");
        resetVerifiedUser = null;
        setResetIdentityEnabled(true);
        if (resetPasswordPanel != null) resetPasswordPanel.setVisible(false);
    }

    void clearForLogout() {
        loginIdField.setText("");
        loginPassField.setText("");
        loginNoticeLabel.setText(" ");
        clearReg();
        clearReset();
        if (successIdLabel != null) successIdLabel.setText("ID:");
        if (successAvatarPreview != null) successAvatarPreview.setImagePath("");
        internalCards.show(internalPanel, LOGIN);
        SwingUtilities.invokeLater(loginIdField::requestFocusInWindow);
    }

    /**
     * Sets the reset identity enabled.
     */
    private void setResetIdentityEnabled(boolean enabled) {
        if (resetIdField != null) resetIdField.setEnabled(enabled);
        if (resetNameField != null) resetNameField.setEnabled(enabled);
        if (resetWorkField != null) resetWorkField.setEnabled(enabled);
        if (resetHomeField != null) resetHomeField.setEnabled(enabled);
    }

    /**
     * Lets the user choose the register avatar.
     */
    private void chooseRegisterAvatar() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            regAvatarPath = fc.getSelectedFile().getAbsolutePath();
            regAvatarPreview.setImagePath(regAvatarPath);
        }
    }

    /**
     * Shows the register success.
     */
    private void showRegisterSuccess(String userId, String avatarPath) {
        successIdLabel.setText("ID: " + userId);
        successAvatarPreview.setImagePath(avatarPath);
        loginIdField.setText(userId);
        loginPassField.setText("");
        internalCards.show(internalPanel, SUCCESS);
    }

    /**
     * Shows an error message to the user.
     */
    private void err(String msg) {
        showStyledDialog(msg);
    }

    /**
     * Checks whether is valid user id.
     */
    private static boolean isValidUserId(String id) {
        return id != null && USER_ID_PATTERN.matcher(id).matches();
    }

    /**
     * Checks whether is valid password.
     */
    private static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Checks whether has unsafe user file chars.
     */
    private static boolean hasUnsafeUserFileChars(String value) {
        return value != null && (value.contains(",") || value.contains("|"));
    }

    /**
     * Handles the same text operation.
     */
    private static boolean sameText(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    /**
     * Handles the user id rule message operation.
     */
    private static String userIdRuleMessage() {
        return "Please choose a User ID with 3-16 characters. Use only letters, numbers, and underscores.";
    }

    /**
     * Handles the password rule message operation.
     */
    static String passwordRuleMessage(String password) {
        StringBuilder message = new StringBuilder("Your password does not meet the requirements yet.\n");

        if (password == null || password.length() < 6 || password.length() > 20) {
            message.append("Please use 6-20 characters for your password.\n");
        }
        if (password == null || !PASSWORD_LETTER_PATTERN.matcher(password).matches()) {
            message.append("Please include at least one letter in your password.\n");
        }
        if (password == null || !PASSWORD_NUMBER_PATTERN.matcher(password).matches()) {
            message.append("Please include at least one number in your password.\n");
        }
        if (password == null || !PASSWORD_ALLOWED_CHARS_PATTERN.matcher(password).matches()) {
            message.append("Please use only the allowed password symbols.\n");
        }

        message.append("Allowed symbols: ! @ # $ % ^ & * . _ -");
        return message.toString();
    }

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

    /**
     * Returns the project root.
     */
    static File getProjectRoot() {
        File cwd = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (File dir = cwd; dir != null; dir = dir.getParentFile()) {
            if (isProjectRoot(dir)) return dir;

            File nested = new File(dir, "slayers_project");
            if (isProjectRoot(nested)) return nested;

            File nestedInFolder = new File(new File(dir, "Data Structure Final"), "slayers_project");
            if (isProjectRoot(nestedInFolder)) return nestedInFolder;
        }
        return cwd;
    }

    /**
     * Checks whether is project root.
     */
    private static boolean isProjectRoot(File dir) {
        return dir != null
                && new File(dir, "src").isDirectory()
                && new File(dir, "image").isDirectory();
    }

    /**
     * Handles the normalize avatar path operation.
     */
    static String normalizeAvatarPath(String avatarPath) {
        if (avatarPath == null || avatarPath.trim().isEmpty()) return "";
        String clean = avatarPath.trim();
        File file = new File(clean);
        if (!file.isAbsolute()) {
            return clean.replace('\\', '/');
        }
        return copyAvatarToImageFolder(clean);
    }

    /**
     * Handles the copy avatar to image folder operation.
     */
    static String copyAvatarToImageFolder(String sourcePath) {
        if (sourcePath == null || sourcePath.isEmpty()) return "";
        try {
            Path source = Paths.get(sourcePath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(source)) return "";

            Path root = getProjectRoot().toPath().toAbsolutePath().normalize();
            Path avatarDir = root.resolve("image").resolve("avatars");
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

    /**
     * Loads the users file.
     */
    private void loadUsersFile() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) {  }
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
                        if (parts.length >= 9) parseFriendNotifications(unescapeUserField(parts[8]), newUser);
                        network.addUser(newUser);
                    } else {
                        User existing = network.getUser(uid);
                        if (!avatarPath.isEmpty()) existing.setAvatarPath(avatarPath);
                        if (parts.length >= 9) parseFriendNotifications(unescapeUserField(parts[8]), existing);
                    }
                }
            }
        } catch (IOException e) {

        }
        if (migratedAvatarPath) rewriteUsersFile(network);
    }

    /**
     * Saves the user to file.
     */
    private void saveUserToFile(String uid, String pw, String name, String work, String home) {

        rewriteUsersFile(network);
    }

    /**
     * Handles the rewrite users file operation.
     */
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
                        + escapeUserField(serializeRemarks(u)) + ","
                        + escapeUserField(serializeFriendNotifications(u)));
            }
        } catch (IOException e) {

        }
    }

    /**
     * Handles the split user record operation.
     */
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

    /**
     * Handles the escape user field operation.
     */
    private static String escapeUserField(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace(",", "\\,");
    }

    /**
     * Handles the unescape user field operation.
     */
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

    /**
     * Serializes the remarks.
     */
    private static String serializeRemarks(User user) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : user.getFriendRemarks().entrySet()) {
            if (entry.getValue() == null || entry.getValue().trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(encode(entry.getValue()));
        }
        return sb.toString();
    }

    /**
     * Parses the remarks.
     */
    private static void parseRemarks(String data, User user) {
        if (data == null || data.trim().isEmpty()) return;
        String[] entries = data.split(";");
        for (String entry : entries) {
            int sep = entry.indexOf(':');
            if (sep <= 0 || sep >= entry.length() - 1) continue;
            user.setFriendRemark(entry.substring(0, sep), decode(entry.substring(sep + 1)));
        }
    }

    /**
     * Serializes the friend notifications.
     */
    private static String serializeFriendNotifications(User user) {
        StringBuilder sb = new StringBuilder();
        for (String userId : user.getFriendNotifications()) {
            if (userId == null || userId.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append(encode(userId.trim()));
        }
        return sb.toString();
    }

    /**
     * Parses the friend notifications.
     */
    private static void parseFriendNotifications(String data, User user) {
        if (data == null || data.trim().isEmpty() || user == null) return;
        String[] entries = data.split(";");
        for (String entry : entries) {
            String userId = decode(entry);
            if (!userId.isEmpty()) user.addFriendNotification(userId);
        }
    }

    /**
     * Handles the encode operation.
     */
    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Handles the decode operation.
     */
    private static String decode(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Handles the centered label operation.
     */
    private JLabel centeredLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font); l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    /**
     * Handles the link label operation.
     */
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

    /**
     * Builds the separator.
     */
    private JPanel buildSeparator(String text) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(FIELD_W, 20));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;

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

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 12, 0, 12);
        JLabel t = new JLabel(text, SwingConstants.CENTER);
        t.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        t.setForeground(TEXT_HINT);
        row.add(t, gbc);

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

    /**
     * Creates the card panel.
     */
    private JPanel createCardPanel(int topPad, int sidePad, int bottomPad) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

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
        card.setBorder(BorderFactory.createEmptyBorder(topPad, sidePad, bottomPad, sidePad));
        return card;
    }

    /**
     * Represents a styled input component used by the login screens.
     *
     * @author Team Slayers
     * @version 1.0
     */
    static class FloatInput extends JPanel {
        private JTextField field;
        private JLabel label;
        private boolean focused, hasText, isPassword;
        private boolean passVisible;
        private static final int INPUT_H = 58;

        /**
         * Constructs a new FloatInput object.
         */
        FloatInput(String labelText, boolean password) {
            this.isPassword = password;
            setLayout(null);
            setOpaque(false);
            setPreferredSize(new Dimension(FIELD_W, INPUT_H));
            setMaximumSize(new Dimension(FIELD_W, INPUT_H));
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
            field.setBorder(BorderFactory.createEmptyBorder(26, 16, 6, password ? 44 : 16));
            field.setBounds(0, 0, FIELD_W, INPUT_H);
            field.setBackground(INPUT_BG);

            label = new JLabel(labelText);
            label.setFont(new Font(FONT_FAMILY, Font.PLAIN, 17));
            label.setForeground(TEXT_HINT);
            label.setBounds(16, 18, FIELD_W - 60, 22);

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

                            g2.drawOval(cx - 8, cy - 5, 16, 10);
                            g2.fillOval(cx - 3, cy - 3, 6, 6);
                        } else {

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
                eye.setBounds(FIELD_W - 40, 15, 28, 28);
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

                setComponentZOrder(eye, 0);
            }
        }

        /**
         * Paints the component.
         */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(INPUT_BG);
            g2.fillRoundRect(0, 0, getWidth(), INPUT_H, 10, 10);

            g2.setColor(new Color(209, 209, 214));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, INPUT_H - 1, 10, 10);

            if (focused) {
                g2.setColor(new Color(0, 102, 204, 64));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(1, 1, getWidth() - 2, INPUT_H - 2, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }

        /**
         * Handles the animate label operation.
         */
        private void animateLabel(boolean up) {
            Timer timer = new Timer(10, null);
            final int[] step = {0};
            final int startY = label.getY();
            final int targetY = up ? 6 : 18;
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

        /**
         * Returns the text.
         */
        public String getText() {
            if (field instanceof JPasswordField)
                return new String(((JPasswordField) field).getPassword());
            return field.getText();
        }

        /**
         * Sets the text.
         */
        public void setText(String t) {
            field.setText(t);
            hasText = !getText().isEmpty();
            setLabelFloating(focused || hasText);
            repaint();
        }

        /**
         * Sets the label floating.
         */
        private void setLabelFloating(boolean up) {
            label.setLocation(label.getX(), up ? 6 : 18);
            label.setFont(new Font(FONT_FAMILY, Font.PLAIN, up ? 13 : 17));
            label.setForeground(up ? BRAND : TEXT_HINT);
        }

        /**
         * Sets the enabled.
         */
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (field != null) field.setEnabled(enabled);
            repaint();
        }
    }

    /**
     * Represents a styled primary action button used by the login screens.
     *
     * @author Team Slayers
     * @version 1.0
     */
    static class PrimaryButton extends JButton {
        private boolean hover;
        private boolean pressed;

        /**
         * Constructs a new PrimaryButton object.
         */
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

        /**
         * Paints the component.
         */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(pressed ? BRAND_DARK : BRAND);
            int arc = 8;
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Displays a preview of the selected avatar image during registration.
     *
     * @author Team Slayers
     * @version 1.0
     */
    static class AvatarPreview extends JPanel {
        private final int size;
        private String imagePath = "";
        private BufferedImage image;

        /**
         * Constructs a new AvatarPreview object.
         */
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

        /**
         * Paints the component.
         */
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

            g2.setColor(HAIRLINE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(clip);
            g2.dispose();
        }
    }
}
