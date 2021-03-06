package Server.Items;

import java.util.logging.Level;

import static General.MultiBomb.LOGGER;

public class ServerBomb extends ServerItem {
    /**
     * The name of the item
     */
    public static final String NAME = "Bomb";
    /**
     * The time till the bomb detonates in seconds
     */
    public static long DETONATION_TIME = 3000;
    /**
     * The total time in seconds
     */
    public static long TOTAL_TIME = 3300;

    /**
     * Run the item logic on the server
     *
     * @param itemCallback callback function that gets passed all fields in a row that might be hit
     * @param m            coordinate on the map
     * @param n            coordinate on the map
     * @param bombSize     the size of the bomb explosion
     */
    public static void serverLogic(ItemCallback itemCallback, int m, int n, int bombSize) {
        LOGGER.config(String.format("Entering: %s %s", ServerBomb.class.getName(), "serverLogic()"));

        // Start new Thread so countdown doesn't block the server
        new Thread(() -> {
            LOGGER.config(String.format("Entering: %s %s", ServerBomb.class.getName(), "BombThread"));

            try {
                // wait for the detonation time
                Thread.sleep(DETONATION_TIME);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Bomb countdown interrupted", e);
                e.printStackTrace();
            }

            LOGGER.info("Detonate Bomb at m=" + m + ", n=" + n + ", with size=" + bombSize);

            // callback for hitting the position of the bomb
            itemCallback.callback(m, n);

            boolean hit_north = false;
            boolean hit_south = false;
            boolean hit_east = false;
            boolean hit_west = false;

            long delay = (TOTAL_TIME - DETONATION_TIME) / bombSize;

            for (int r = 1; r <= bombSize; r++) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // fill all four rows with the according positions
                if (!hit_north) hit_north = itemCallback.callback(m - r, n);
                if (!hit_south) hit_south = itemCallback.callback(m + r, n);
                if (!hit_east) hit_east = itemCallback.callback(m, n + r);
                if (!hit_west) hit_west = itemCallback.callback(m, n - r);
            }

            LOGGER.config(String.format("Exiting: %s %s", ServerBomb.class.getName(), "BombThread"));
        }).start();

        LOGGER.config(String.format("Exiting: %s %s", ServerBomb.class.getName(), "serverLogic()"));
    }
}
