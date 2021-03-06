package Server.Messages.Socket;

import Game.Models.Direction;
import Server.Messages.Message;

/**
 * Basic position model
 */
public class Position extends Message {
    /**
     * The position on the x-axis
     */
    public float x = 1;
    /**
     * The position on the y-axis
     */
    public float y = 1;
    /**
     * The direction
     */
    public Direction direction = Direction.SOUTH;
    /**
     * The player ID
     */
    public String playerId;
    /**
     * True if the player is moving in the corresponding direction
     */
    public boolean moving = false;

    /**
     * Constructor
     */
    public Position() {
        // Initialize message with type
        super(Message.POSITION_TYPE);
    }

    /**
     * Constructor
     *
     * @param x coordinate
     * @param y coordinate
     */
    public Position(float x, float y) {
        // Initialize message with type
        super(Message.POSITION_TYPE);
        this.x = x;
        this.y = y;
    }
}
