package Server;

import Server.Messages.Socket.Map;
import Server.Messages.ErrorMessage;
import Server.Messages.Message;
import Server.Messages.Socket.CloseConnection;
import Server.Messages.Socket.GameState;
import Server.Messages.Socket.LobbyState;
import Server.Messages.Socket.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PlayerConnection extends Thread {
    /**
     * Name of the player
     */
    public final String name;
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
     * Color of the player
     */
    public int color;
    /**
     * The last position update the server received
     */
    public Position lastPosition;
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
                    lastPosition = (Position) msg;
                    lastPosition.playerId = name;
                }
                break;
        }
    }

    /**
     * Send message
     *
     * @param message message to send
     */
    public void send(Message message) {
        synchronized (out) {
            out.println(message.toJson());
        }
    }

    /**
     * Close socket connection to client
     */
    public void close() {
        alive = false;
    }
}
