package Server;

import Server.Messages.ErrorMessage;
import Server.Messages.Message;
import Server.Messages.Socket.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static General.MultiBomb.LOGGER;

public class PlayerConnection extends Thread {
    /**
     * Name of the player
     */
    public final String name;
    /**
     * List of all ItemActions that occurred
     */
    public final List<ItemAction> itemActions;
    /**
     * TCP socket connection to the client
     */
    private final Socket socket;
    /**
     * Lobby the player is in
     */
    private final Lobby lobby;
    /**
     * Output stream
     */
    private final PrintWriter out;
    /**
     * Input stream
     */
    private final BufferedReader in;
    /**
     * Queue for outgoing messages
     */
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>(1000);
    /**
     * Color of the player
     */
    public int color;
    /**
     * The last position update the server received
     */
    public volatile Position lastPosition = new Position(-5, -5);
    /**
     * If the client is prepared and ready to start the game
     */
    public boolean preparationReady = false;
    /**
     * Indicate if PlayerConnection is still alive
     */
    private boolean alive;

    /**
     * Constructor
     *
     * @param socket     socket connection with the client
     * @param lobby      lobby the player is in
     * @param playerName name of the player
     * @throws IOException if there are problems with the socket or the in-/output streams
     */
    public PlayerConnection(Socket socket, Lobby lobby, String playerName) throws IOException {
        this.socket = socket;
        this.lobby = lobby;
        this.name = playerName;

        this.itemActions = new ArrayList<>();

        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    @Override
    public void run() {
        alive = lobby.addPlayer(this);

        if (!alive && lobby.getPlayerColors().containsKey(name)) {
            send(new ErrorMessage("Name already taken, please choose a different one!"));
        }

        new Thread(() -> {
            Message msg;
            while (alive) {
                try {
                    msg = messageQueue.take();

                    synchronized (out) {
                        out.println(msg.toJson());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        String jsonMessage;
        while (alive) {
            try {
                synchronized (in) {
                    // readLine() return null if socket connection was closed from other end
                    if ((jsonMessage = in.readLine()) == null) break;
                }
            } catch (IOException e) {
                break;
            }

            Message message = Message.fromJson(jsonMessage);

            handleMessage(message);
        }

        send(new CloseConnection());

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        lobby.removePlayer(this);
    }

    /**
     * Handle incoming message
     *
     * @param msg message to handle
     */
    private void handleMessage(Message msg) {
        if (!msg.type.equals(Message.POSITION_TYPE)) {
            LOGGER.config(String.format("Entering: %s %s", PlayerConnection.class.getName(), "handleMessage(" + msg.type + ")"));
        }

        switch (msg.type) {
            case Message.LOBBY_STATE_TYPE:
                synchronized (lobby) {
                    if (lobby.state == Lobby.WAITING) {
                        if (this == lobby.host) {
                            // Change host or game mode of lobby
                            lobby.updateLobbyState((LobbyState) msg);
                        } else {
                            this.send(new ErrorMessage("You must be host to perform this action!"));
                        }
                    }
                }
                break;
            case Message.MAP_TYPE:
                synchronized (lobby) {
                    if (lobby.state == Lobby.WAITING) {
                        if (this == lobby.host) {
                            // Start game by sending game map
                            lobby.prepareGame((Map) msg);
                        } else {
                            this.send(new ErrorMessage("You must be host to perform this action!"));
                        }
                    }
                }
                break;
            case Message.GAME_STATE_TYPE:
                GameState gameState = (GameState) msg;
                synchronized (lobby) {
                    if (lobby.state == Lobby.GAME_STARTING && gameState.state == GameState.PREPARING) {
                        // Client is displaying game and is ready to start
                        preparationReady = true;
                        // Start game if all other players are ready as well
                        lobby.startGame();
                    }
                }
                break;
            case Message.POSITION_TYPE:
                if (lobby.state == Lobby.IN_GAME) {
                    // update last position
                    Position newPosition = (Position) msg;
                    newPosition.playerId = name;
                    lastPosition = newPosition;
                }
                break;
            case Message.ITEM_ACTION_TYPE:
                // add item action to itemActions
                synchronized (itemActions) {
                    itemActions.add((ItemAction) msg);
                }
                break;
        }

        if (!msg.type.equals(Message.POSITION_TYPE)) {
            LOGGER.config(String.format("Exiting: %s %s", PlayerConnection.class.getName(), "handleMessage(" + msg.type + ")"));
        }
    }

    /**
     * Send message
     *
     * @param message message to send
     */
    public void send(Message message) {
        if (!messageQueue.offer(message)) {
            close();
        }
    }

    /**
     * Close socket connection to client
     */
    public void close() {
        alive = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
