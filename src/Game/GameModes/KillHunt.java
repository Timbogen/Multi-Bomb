package Game.GameModes;

import Game.Models.Field;
import Server.Items.ServerProtection;
import Server.Messages.Message;
import Server.Messages.Socket.PlayerState;
import Server.Messages.Socket.Respawn;
import Server.Models.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static General.MultiBomb.LOGGER;

public class KillHunt extends GameMode {
    /**
     * The description of the mode
     */
    public static final String DESCRIPTION = "The first one to get 10 kills is the winner!";

    /**
     * Constructor
     */
    public KillHunt() {
        super(GameMode.KILL_HUNT, DESCRIPTION, Field.getAllItems(false));
    }

    @Override
    public Optional<String> calculateWinner() {
        LOGGER.config(String.format("Entering: %s %s", KillHunt.class.getName(), "calculateWinner()"));

        // Get list of all players that are alive
        List<PlayerState> alivePlayers = players.values().stream().filter(PlayerState::isAlive)
                .collect(Collectors.toList());

        // If only one player is alive, they are winner
        if (alivePlayers.size() == 1) {
            return alivePlayers.stream().findFirst().map(ps -> ps.playerId);
        }

        LOGGER.config(String.format("Exiting: %s %s", KillHunt.class.getName(), "calculateWinner()"));
        // Return the playerId of the first player to reach 10 kills, empty Optional if no player has 10 kills yet
        return alivePlayers.stream().filter(ps -> ps.kills >= 10).findFirst().map(ps -> ps.playerId);
    }

    @Override
    public List<Message> handleHit(Player player, Player from) {
        LOGGER.config(String.format("Exiting: %s %s", KillHunt.class.getName(), "handleHit()"));

        List<Message> result = new ArrayList<>();

        if (!player.isProtected) {
            // player got hit and spawns again
            result.add(new Respawn(player.name));
            ServerProtection.serverLogic(ServerProtection.STANDARD_DURATION + ServerProtection.DIE_DURATION, player);

            LOGGER.info(String.format("Player %s got hit by %s and respawns", player.name, from.name));

            if (!player.name.equals(from.name)) {
                // update kill for other player
                from.playerState.kills++;

                // notify players about the kill
                result.add(from.playerState);

            } else if (from.playerState.kills > 0) {
                // decrement kills of player because he hit himself
                from.playerState.kills--;

                // notify players about the kill
                result.add(from.playerState);
            }

            LOGGER.info(String.format("Player %s killed %s", from.name, player.name));
        }

        LOGGER.config(String.format("Exiting: %s %s", KillHunt.class.getName(), "handleHit()"));
        return result;
    }
}
