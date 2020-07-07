package Menu;

import General.MB;
import General.Shared.*;
import Menu.Dialogs.EnterLobbyName;
import Menu.Dialogs.EnterPlayerName;
import Server.DetectLobby;
import Server.Messages.ErrorMessage;
import Server.Messages.Message;
import Server.Messages.REST.CreateLobby;
import Server.Messages.REST.JoinLobby;
import Server.Messages.REST.LobbyInfo;
import Server.Server;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static Menu.DetailedLobbyView.BUTTON_WIDTH;
import static Menu.DetailedLobbyView.MARGIN;
import static Menu.ServerView.REFRESH_TIME;

/**
 * This class provides an lobby overview
 */
public class LobbyView extends MBPanel {

    /**
     * Selected lobby
     */
    private static int selectedLobby;
    /**
     * Address of selected Server
     */
    private final String serverAddress;
    /**
     * Lobbies found in lobbyInfo
     */
    private final ArrayList<LobbyListItem> lobbyCache = new ArrayList<>();
    /**
     * Lobby info
     */
    private LobbyInfo lobbyInfo;
    /**
     * List view for lobbies
     */
    private MBListView<LobbyListItem> listView = new MBListView<>();
    /**
     * Spinner
     */
    private MBSpinner spinner;
    /**
     * Buttons
     */
    private MBButton back, join, create;
    /**
     * Player name
     */
    private String playerName = "";
    /**
     * True if running
     */
    private boolean running = false;

    /**
     * Constructor
     *
     * @param address of the server
     */
    public LobbyView(String address) {
        super(true);
        setupLayout();
        serverAddress = address;
        if (!MB.settings.playerName.isEmpty()) {
            playerName = MB.settings.playerName;
        }
    }

    /**
     * Setup the layout
     */
    public void setupLayout() {
        //The title
        MBLabel title = new MBLabel("Lobby Overview", SwingConstants.CENTER, MBLabel.H1);
        addComponent(title, () -> title.setBounds(
                getWidth() / 2 - 150,
                32,
                300,
                40)
        );

        //Create scrollable listView
        listView = new MBListView<>();
        MBScrollView scroll = new MBScrollView(listView);
        addComponent(scroll, () -> scroll.setBounds(
                getWidth() / 2 - (getHeight() + MARGIN + BUTTON_WIDTH) / 2,
                96,
                getHeight(),
                getHeight() - 160));

        //loading spinner
        spinner = new MBSpinner();
        addComponent(spinner, () ->
                spinner.setBounds(
                        scroll.getX() + scroll.getWidth() / 2 - 50,
                        scroll.getY() + scroll.getHeight() / 2 - 50,
                        100,
                        100)
        );

        // The background
        MBBackground background = new MBBackground(new Color(0, 0, 0, 0.2f));
        addComponent(background, () -> background.setBounds(
                scroll.getX() - 4,
                scroll.getY() - 4,
                scroll.getWidth() + 8,
                scroll.getHeight() + 8
        ));

        // Join button
        join = new MBButton("Join");
        join.addActionListener(e -> {
            if (selectedLobby >= 0 && selectedLobby < lobbyInfo.lobbies.length) {
                showDialog(
                        new EnterPlayerName(
                        false,
                                lobbyInfo.lobbies[selectedLobby].name,
                                this),
                        () -> {}
                );
            } else {
                toastError("No lobby", "selected!");
            }
        });

        addComponent(join, () -> join.setBounds(
                scroll.getX() + scroll.getWidth() + MARGIN,
                scroll.getY() - 4,
                DetailedLobbyView.BUTTON_WIDTH,
                DetailedLobbyView.BUTTON_HEIGHT
        ));

        // Create button
        create = new MBButton("Create");
        create.addActionListener(e -> {
            //set player name
            showDialog(new EnterLobbyName(this), () -> {});
        });

        addComponent(create, () -> create.setBounds(
                scroll.getX() + scroll.getWidth() + MARGIN,
                scroll.getY() + 46,
                DetailedLobbyView.BUTTON_WIDTH,
                DetailedLobbyView.BUTTON_HEIGHT
        ));


        // Back button
        back = new MBButton("Back");
        back.addActionListener(e -> {
            running = false;
            MB.show(new ServerView(), false);
        });
        addComponent(back, () -> back.setBounds(
                scroll.getX() + scroll.getWidth() + MARGIN,
                scroll.getY() + scroll.getHeight() - DetailedLobbyView.BUTTON_HEIGHT + 4,
                DetailedLobbyView.BUTTON_WIDTH,
                DetailedLobbyView.BUTTON_HEIGHT
        ));
        addButtonGroup(back, join, create);


    }


    /**
     * Method that is executed when panel is visible
     */
    @Override
    public void afterVisible() {
        //Thread to detect lobbies on server
        new Thread(() -> {
            running = true;
            while (true) {
                //get lobby info from server
                lobbyInfo = DetectLobby.getLobbyInfo(serverAddress);
                if (lobbyInfo == null) {
                    toastError("Server not", "available");
                    running = false;
                    MB.show(new ServerView(), false);
                }
                LobbyInfo.SingleLobbyInfo[] lobbies = lobbyInfo.lobbies;
                //Create LobbyListItem for every lobby in LobbyInfo
                for (LobbyInfo.SingleLobbyInfo lobby : lobbies) {
                    LobbyListItem listItem = new LobbyListItem(lobby.name, lobby.players, lobby.gameMode, lobby.status);
                    lobbyCache.add(listItem);
                }
                //clear ListView and show new lobbies
                listView.addMissingItems(lobbyCache);
                lobbyCache.clear();
                //Set button visibility
                spinner.setVisible(false);
                back.setVisible(true);
                //check if Thread still needed
                if (!running) {
                    break;
                }
                //Wait 2 seconds
                try {
                    Thread.sleep(REFRESH_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Choose a name for the player
     *
     * @param name of the player
     */
    public void setPlayerName(String name) {
        this.playerName = name;
        MB.settings.playerName = name;
        MB.settings.saveSettings();
    }

    /**
     * JoinLobby Method
     */
    public void joinLobby(boolean create, String name) {
        try {
            // Setup the request
            HttpURLConnection urlConn;
            URL mUrl = new URL("http://" + serverAddress + ":" + Server.HTTP_PORT + "/lobby");
            urlConn = (HttpURLConnection) mUrl.openConnection();
            urlConn.setDoOutput(true);

            if (create) {
                // Create a lobby
                CreateLobby message = new CreateLobby();
                message.playerID = playerName;
                message.lobbyName = name;
                String query = message.toJson();
                urlConn.setRequestProperty("Content-Length", Integer.toString(query.length()));
                urlConn.getOutputStream().write(query.getBytes(StandardCharsets.UTF_8));
            } else {
                // Join a lobby
                JoinLobby message = new JoinLobby();
                message.playerID = playerName;
                message.lobbyName = name;
                String query = message.toJson();
                urlConn.setRequestProperty("Content-Length", Integer.toString(query.length()));
                urlConn.getOutputStream().write(query.getBytes(StandardCharsets.UTF_8));
            }

            // Check if the server responded
            if (urlConn.getResponseCode() == 200) {
                running = false;

                // Show the detailed lobby view
                MB.show(
                        new DetailedLobbyView(
                                playerName,
                                name,
                                serverAddress,
                                128
                        ),
                        false
                );
            } else {
                // Show the error message
                String body = new BufferedReader(new InputStreamReader(urlConn.getErrorStream())).readLine();
                ErrorMessage error = (ErrorMessage) Message.fromJson(body);
                toastError(error.error);
            }

            // Close the connection
            urlConn.disconnect();
        } catch (IOException e) {
            MB.show(new ServerView(), false);
            MB.activePanel.toastError("Server communication failed!");
            e.printStackTrace();
        }
    }

    private class LobbyListItem extends MBListView.Item {
        /**
         * Count of connected players
         */
        int players;
        /**
         * Label for lobby name
         */
        MBLabel nameLabel;
        /**
         * Label for lobby description
         */
        MBLabel descriptionLabel;

        /**
         * Constructor
         *
         * @param name      of the lobby
         * @param players   amount of player
         * @param gameMode  the active game mode
         * @param status    state of the game
         */
        public LobbyListItem(String name, int players, String gameMode, String status) {
            super(name);
            this.name = name;
            this.players = players;
            setBounds(0, 0, 400, 100);
            setLayout(null);

            // Setup the name
            nameLabel = new MBLabel(name);
            nameLabel.setBold();
            add(nameLabel);

            // Setup the description
            String description = "Players " + players + "/8 - Gamemode" + gameMode + " - Status " + status;
            descriptionLabel = new MBLabel(MBLabel.DESCRIPTION, description);
            add(descriptionLabel);
        }

        @Override
        public void onResize(int y, int width) {
            setBounds(0, y, width, 50);
            nameLabel.setBounds(12, 6, width - 32, 20);
            descriptionLabel.setBounds(12, 26, width - 32, 20);
        }

        @Override
        public void onSelected(int index) {
            LobbyView.selectedLobby = index;
            MB.frame.revalidate();
            MB.frame.repaint();
        }

        /**
         * Paint the border
         */
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            MB.settings.enableAntiAliasing(g);
            if (selectedLobby >= 0 && selectedLobby < lobbyInfo.lobbies.length
                    && lobbyInfo.lobbies[selectedLobby].name.equals(name)) {
                g.setColor(Color.WHITE);
                g.drawRoundRect(
                        0,
                        0,
                        getWidth() - 1,
                        getHeight() - 1,
                        MBBackground.CORNER_RADIUS,
                        MBBackground.CORNER_RADIUS
                );
            }
        }
    }
}