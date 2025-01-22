package me.flyingtaco725.tempDeathBanner.CommandHandlers;

import me.flyingtaco725.tempDeathBanner.PlayerInfo.PlayerInfo;
import me.flyingtaco725.tempDeathBanner.tempDeathBanner;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

public class TdbCommandHandler {
    private final tempDeathBanner plugin;

    public TdbCommandHandler(tempDeathBanner plugin) {
        this.plugin = plugin;
    }

    /*
        FUNCTION: handleCommand()
        PURPOSE: the purpose of this function is to handle the tdb command, and its
                 many possible arguments
     */
    public boolean handleCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("tdb")){
            if(!sender.hasPermission("tempdeathbanner.use")) {
                sender.sendMessage("You don't have permission to use this command.");
                return true;
            }
            if(args.length == 0) {
                sender.sendMessage("Usage: /tdb [resetdeaths|resetdeathsall|showdeathboard|closedeathboard]");
                return true;
            }
            switch(args[0].toLowerCase()) {
                case "resetdeaths":
                    if(!sender.hasPermission("tempdeathbanner.resetdeaths")){
                        sender.sendMessage("§e[§lTempDeathBanner§l] §cYou don't have permission to reset deaths for a specific player.");
                        return true;
                    }
                    if (args.length < 2) {
                        sender.sendMessage("Usage: /tdb resetdeaths <playerName>");
                        return true;
                    }
                    resetDeaths(sender, args[1]);
                    return true;

                case "resetdeathsall":
                    if (!sender.hasPermission("tempdeathbanner.resetdeathsall")) {
                        sender.sendMessage("§e[§lTempDeathBanner§l] §cYou don't have permission to reset deaths for all players.");
                        return true;
                    }
                    resetDeathsAll(sender);
                    return true;

                case "showdeathboard":
                    // show player the death board
                    if (!sender.hasPermission("tempdeathbanner.showdeathboard")){
                        sender.sendMessage("§e[§lTempDeathBanner§l] §cYou don't have permission to open death board.");
                        return true;
                    }
                    showDeathBoard(sender);
                    sender.sendMessage("§e[§lTempDeathBanner§l] §aOpened the death board.");
                    return true;

                case "closedeathboard":
                    // Close the player's death board
                    if (!sender.hasPermission("tempdeathbanner.closedeathboard")){
                        sender.sendMessage("§e[§lTempDeathBanner§l] §cYou don't have permission to close deathboard.");
                        return true;
                    }
                    Player player = (Player) sender;
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                    sender.sendMessage("§e[§lTempDeathBanner§l] §aClosed the death board.");
                    return true;

                default:
                    return true;
            }
        }
        return false;
    }

    /*
        FUNCTION: resetDeaths()
        PURPOSE: the purpose of this function is to reset a specific players
                 death count and penalties back to the initial number specified
                 in the config
     */
    private void resetDeaths(CommandSender sender, String playerName){
        Player player = Bukkit.getPlayer(playerName);
        if(player != null){
            UUID uuid = player.getUniqueId();
            resetPlayerDeathCounter(player);
            player.sendMessage("§e[§lTempDeathBanner§l] §c" + playerName + "'s §adeath count has been set back to §c0");
            plugin.banManager.saveList(plugin.playerDataFile, plugin.banManagementList);
        }
        else{
            sender.sendMessage("§e[§lTempDeathBanner§l] §aPlayer with name §c" + playerName + " §ais offline or does not exist.");
        }
    }
    /*
        FUNCTION: resetPlayerDeathCounter()
        PURPOSE: loop through banManagementList and reset a players death counter to 0
     */
    private void resetPlayerDeathCounter(Player playerOBJ)
    {
        for (PlayerInfo player : plugin.banManagementList) {
            if (player.getPlayerUUID().equals(playerOBJ.getUniqueId())) {
                player.setDeathCount(0);
                player.setLastMilli(0);
            }
        }
    }
    /*
        FUNCTION: resetDeathsAll()
        PURPOSE: resets all players back to default settings
     */
    private void resetDeathsAll(CommandSender sender){
        for (PlayerInfo player : plugin.banManagementList){
            player.setDeathCount(0);
            player.setLastMilli(0);
        }
        plugin.banManager.saveList(plugin.playerDataFile, plugin.banManagementList);
        plugin.getServer().broadcastMessage("§e[§lTempDeathBanner§l] §cAll Players §ahave had their death count returned to §c0");
    }
    /*
        FUNCTION: showDeathBoard()
        PURPOSE: shows a scoreboard of player deaths to the user
     */
    public void showDeathBoard(CommandSender sender)
    {
        ScoreboardManager manager;
        Scoreboard deathboard;
        Objective objective;
        Player p = (Player) sender;

        // scoreboard create
        manager = Bukkit.getScoreboardManager();
        deathboard = manager.getNewScoreboard();
        objective = deathboard.registerNewObjective("deathboard", "dummy", plugin.scoreboardTitle);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        loadDeathBoard(manager, deathboard, objective, p);
    }

    /*
        FUNCTION: loadDeathBoard()
        PURPOSE: responsible for loading the data for the showDeathBoard command
     */
    public void loadDeathBoard(ScoreboardManager manager, Scoreboard deathboard, Objective objective, Player p){
        // Sort the players based on death count and then by name if there's a tie
        Collections.sort(plugin.banManagementList, new Comparator<PlayerInfo>() {
            @Override
            public int compare(PlayerInfo player1, PlayerInfo player2) {
                // Sort by death count in descending order
                int deathCountComparison = Integer.compare(player2.getDeathCount(), player1.getDeathCount());

                // If death counts are equal, sort by player name alphabetically
                if (deathCountComparison == 0) {
                    return player1.getPlayerName().compareToIgnoreCase(player2.getPlayerName());
                }

                return deathCountComparison;
            }
        });

        // Take the first 5 players from the sorted list or less if there are fewer than 5 players
        int numPlayers = Math.min(5, plugin.banManagementList.size());
        for (int i = 0; i < numPlayers; i++) {
            PlayerInfo player = plugin.banManagementList.get(i);
            String playerName = player.getPlayerName();
            int deathCount = player.getDeathCount();

            // Set the score for the player in the scoreboard
            Score score = objective.getScore("§a" + playerName);
            score.setScore(deathCount);
        }

        p.setScoreboard(deathboard);

    }
}
