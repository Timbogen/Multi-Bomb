package General.Shared;

import Game.Battleground;
import Game.Models.Field;
import General.MB;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class MBImage {
    /**
     * The resize handling
     */
    private final MBPanel.ComponentResize resize;
    /**
     * The width
     */
    public int width;
    /**
     * The height
     */
    public int height;
    /**
     * The shown image
     */
    public Image image;
    /**
     * The actual image
     */
    public Image original;

    /**
     * Constructor for global images that are used all over the project.
     * That's why the resize event is registered to the frame instead of the active panel.
     *
     * @param relativePath to the picture
     */
    public MBImage(String relativePath) {
        this.resize = () -> {
            if ((float) MB.frame.getWidth() / MB.frame.getHeight() > 21f / 9) {
                this.width = MB.frame.getWidth();
                this.height = (int) (9f / 21 * MB.frame.getWidth());
            } else {
                this.height = MB.frame.getHeight();
                this.width = (int) (21f / 9 * MB.frame.getHeight());
            }
        };
        initialize(relativePath, true, null);
    }

    /**
     * Constructor
     *
     * @param relativePath to the image
     * @param square       true if the image is a square
     * @param parent       the image's size depends on
     */
    public MBImage(String relativePath, boolean square, MBPanel parent) {
        if (square) {
            this.resize = () -> {
                this.width = Battleground.fieldSize;
                this.height = Battleground.fieldSize;
            };
        } else {
            this.resize = () -> {
                width = (int) ((Field.WIDTH) * Battleground.ratio);
                height = (int) ((Field.HEIGHT) * Battleground.ratio);
            };
        }
        initialize(relativePath, false, parent);
    }

    /**
     * Constructor
     *
     * @param relativePath to the image
     * @param parent       the image's size depends on
     * @param resize       the handler
     */
    public MBImage(String relativePath, MBPanel parent, MBPanel.ComponentResize resize) {
        this.resize = resize;
        initialize(relativePath, false, parent);
    }

    /**
     * Initialize the image
     */
    private void initialize(String relativePath, boolean onFrame, MBPanel parent) {
        // Load the image
        try {
            this.original = ImageIO.read(MBImage.class.getResource("/Resources/" + relativePath));
        } catch (IOException e) {
            this.original = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
        this.image = this.original;

        // Listen for resize events
        if (onFrame) {
            MB.frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    refresh();
                }
            });
        } else {
            if (parent != null) {
                parent.addResizeEvent(this::refresh);
            } else {
                MB.activePanel.addResizeEvent(this::refresh);
            }
        }
        refresh();
    }

    /**
     * Resize and rescale the image
     */
    public void refresh() {
        try {
            // Recalculate the measurements
            resize.resize();
            // Rescale the image
            rescale();
        } catch (NullPointerException e) {
            // Set to default values
            this.width = Battleground.fieldSize;
            this.height = Battleground.fieldSize;
            rescale();
        }
    }

    /**
     * Cut out a certain part of an image
     *
     * @param x      source x position
     * @param y      source y position
     * @param width  width of the image part
     * @param height height of the image part
     * @return the part of the image
     */
    public Image getSub(int x, int y, int width, int height) {
        // Create a transparent buffered image
        BufferedImage bufferedImage = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB
        );

        // Draw the image on to the buffered image
        Graphics2D bGr = bufferedImage.createGraphics();
        bGr.drawImage(
                image,
                0,
                0,
                width,
                height,
                x,
                y,
                x + width,
                y + height,
                null
        );
        bGr.dispose();
        return bufferedImage;
    }

    /**
     * Actively rescale an image
     *
     * @param width  new width
     * @param height new height
     */
    public void rescale(int width, int height) {
        this.width = width;
        this.height = height;
        rescale();
    }

    /**
     * Rescale the image without losing quality
     */
    private void rescale() {
        Image scaled;

        // If image width or height is zero return transparent image
        if (width <= 0 || height <= 0) {
            return;
        }

        // Resize the image
        scaled = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        // Check if resizing was successful
        if (scaled != null) {
            this.image = scaled;
        }
    }
}
