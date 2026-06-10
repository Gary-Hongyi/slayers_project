import javax.swing.*;

/**
 * Main entry point for the SnapTok Social Network application.
 * Launches the GUI and initializes the social network data model.
 *
 * @author SnapTok Development Team
 * @version 1.0
 */
public class SocialMediaApp {

    /**
     * Main method that launches the social network application.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Set system look and feel for native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default look and feel
        }

        // Launch GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            SocialNetwork network = new SocialNetwork();
            MainGUI gui = new MainGUI(network);
            gui.setVisible(true);
        });
    }
}
