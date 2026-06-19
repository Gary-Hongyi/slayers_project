import javax.swing.*;

/**
 * Starts the SnapTok application and configures the common Swing settings.
 *
 * @author Team Slayers
 * @version 1.0
 */
public class SocialMediaApp {

    /**
     * Starts the program from the command line.
     */
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {

        }
        configureEnglishDialogText();

        SwingUtilities.invokeLater(() -> {
            SocialNetwork network = new SocialNetwork();
            MainGUI gui = new MainGUI(network);
            gui.setVisible(true);
        });
    }

    /**
     * Configures Swing dialog button text in English.
     */
    private static void configureEnglishDialogText() {
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.cancelButtonText", "Cancel");
        UIManager.put("OptionPane.yesButtonText", "Yes");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("FileChooser.openButtonText", "Open");
        UIManager.put("FileChooser.saveButtonText", "Save");
        UIManager.put("FileChooser.cancelButtonText", "Cancel");
        UIManager.put("FileChooser.lookInLabelText", "Look in");
        UIManager.put("FileChooser.fileNameLabelText", "File name");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Files of type");
    }
}
