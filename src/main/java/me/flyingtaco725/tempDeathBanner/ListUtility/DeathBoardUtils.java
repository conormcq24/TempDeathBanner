package me.flyingtaco725.tempDeathBanner.ListUtility;

import me.flyingtaco725.tempDeathBanner.PlayerInfo.PlayerInfo;
import me.flyingtaco725.tempDeathBanner.tempDeathBanner;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Collections;
import java.util.Comparator;

public class DeathBoardUtils {
    private final tempDeathBanner plugin;

    public DeathBoardUtils(tempDeathBanner plugin) {
        this.plugin = plugin;
    }

    /*
        FUNCTION: showDeathBoard()
        PURPOSE: the purpose of this function is to handle creating a scoreboard for the player,
                 it sets the title using configuartion variable and calls the method loadDeathBoard,
                 to load death data before setting the players scoreboard data
     */
    public void showDeathBoard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard deathboard = manager.getNewScoreboard();
        Objective objective = deathboard.registerNewObjective("deathboard", "dummy", plugin.scoreboardTitle);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        loadDeathBoard(manager, deathboard, objective);
        player.setScoreboard(deathboard);
    }

    /*
        FUNCTION: loadDeathBoard()
        PURPOSE: a subfunction of showDeathBoard, meant to load data related to player deaths
     */
    private void loadDeathBoard(ScoreboardManager manager, Scoreboard deathboard, Objective objective) {
        Collections.sort(plugin.banManagementList, Comparator
                .comparingInt(PlayerInfo::getDeathCount).reversed()
                .thenComparing(PlayerInfo::getPlayerName, String.CASE_INSENSITIVE_ORDER));

        int numPlayers = Math.min(5, plugin.banManagementList.size());
        for (int i = 0; i < numPlayers; i++) {
            PlayerInfo player = plugin.banManagementList.get(i);
            Score score = objective.getScore("§a" + player.getPlayerName());
            score.setScore(player.getDeathCount());
        }
    }

    /*
        FUNCTION: setPlayerDeathBoardVisibility()
        PURPOSE: a function that sets the boolean variable to a unique players death board visibility
     */
    public void setPlayerDeathBoardVisibility(Player player, boolean visibility) {
        plugin.banManagementList.stream()
                .filter(p -> p.getPlayerUUID().equals(player.getUniqueId()))
                .findFirst()
                .ifPresent(p -> p.setDeathboardVisibility(visibility));
    }
}
