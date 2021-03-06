package General.Shared;

import General.MB;

import javax.swing.*;
import java.awt.*;

public class MBInput extends JTextField {

    /**
     * The font for the input field
     */
    private final static Font textFont = new Font(MBLabel.FONT_NAME, Font.PLAIN, MBLabel.NORMAL);
    /**
     * Padding of the cursor
     */
    private static final int PADDING = 8;

    /**
     * Constructor
     */
    public MBInput() {
        setFont(textFont);
        setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
    }

    /**
     * Paint the border
     *
     * @param g the graphics
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        MB.settings.enableAntiAliasing(g);
        g.setColor(MBButton.BACKGROUND_COLOR);
        g.fillRoundRect(
                0,
                0,
                getWidth() - 1,
                getHeight() - 1,
                MBBackground.CORNER_RADIUS,
                MBBackground.CORNER_RADIUS
        );
    }
}
