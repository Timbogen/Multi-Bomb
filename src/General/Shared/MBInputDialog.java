package General.Shared;

import General.MB;

import javax.swing.*;
import java.awt.*;

public class MBInputDialog extends JPanel {
    /**
     * The margin
     */
    private static final int MARGIN = 16;
    /**
     * Button measurements
     */
    private static final int BUTTON_WIDTH = 100, BUTTON_HEIGHT = 30;
    /**
     * The maximum name length for a map
     */
    private static final int MAX_LENGTH = 24;

    /**
     * Constructor
     *
     * @param title       of the dialog
     * @param defaultText to be shown in the input field
     * @param event       that is triggered whenever the confirm button is used
     */
    public MBInputDialog(String title, String defaultText, OnConfirm event) {
        setupDialog(title, defaultText, MAX_LENGTH, event);
    }

    /**
     * Constructor
     *
     * @param title       of the dialog
     * @param defaultText to be shown in the input field
     * @param maxLength   max length of the name
     * @param event       that is triggered whenever the confirm button is used
     */
    public MBInputDialog(String title, String defaultText, int maxLength, OnConfirm event) {
        setupDialog(title, defaultText, maxLength, event);
    }

    /**
     * Constructor
     *
     * @param title       of the dialog
     * @param defaultText to be shown in the input field
     * @param event       that is triggered whenever the confirm button is used
     */
    public void setupDialog(String title, String defaultText, int maxLength, OnConfirm event) {
        setLayout(null);
        setBackground(Color.white);
        setBounds(0, 0, 3 * MARGIN + 2 * BUTTON_WIDTH, 50 + 2 * BUTTON_HEIGHT + MARGIN);

        // The title
        MBLabel titleLabel = new MBLabel(MBLabel.H2, title);
        titleLabel.setFontColor(Color.BLACK);
        titleLabel.setBounds(MARGIN, MARGIN, getWidth() - 2 * MARGIN, 20);
        add(titleLabel);

        // The input field
        MBInput input = new MBInput();
        input.setText(defaultText);
        input.setBounds(MARGIN, 46, getWidth() - 2 * MARGIN, BUTTON_HEIGHT);
        add(input);

        // The cancel button
        MBButton cancel = new MBButton("Cancel");
        cancel.setBounds(MARGIN, 84, BUTTON_WIDTH, BUTTON_HEIGHT);
        cancel.addActionListener(e -> MB.activePanel.closeDialog());
        add(cancel);

        // The confirm button
        MBButton confirm = new MBButton("Confirm");
        confirm.setBounds(2 * MARGIN + BUTTON_WIDTH, 84, BUTTON_WIDTH, BUTTON_HEIGHT);
        confirm.addActionListener(e -> {
            String text = input.getText();
            if (text.isEmpty()) {
                MB.activePanel.toastError("Name is empty!");
                return;
            } else if (text.length() > maxLength) {
                MB.activePanel.toastError(
                        "This name exceeds the",
                        "character limit of " + maxLength + " by " + (text.length() - maxLength) + "!"
                );
                return;
            }
            event.onConfirm(input.getText());
        });
        add(confirm);
    }


    /**
     * Event that is triggered when confirm button is used
     */
    public interface OnConfirm {
        void onConfirm(String text);
    }
}
