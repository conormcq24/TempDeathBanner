package me.flyingtaco725.tempDeathBanner.CommandHandlers;

import me.flyingtaco725.tempDeathBanner.PlayerInfo.PlayerInfo;
import me.flyingtaco725.tempDeathBanner.tempDeathBanner;
import me.flyingtaco725.tempDeathBanner.ListUtility.DeathBoardUtils;
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
    private final DeathBoardUtils deathBoardUtils;

    public TdbCommandHandler(tempDeathBanner plugin) {
        this.plugin = plugin;
        this.deathBoardUtils = new DeathBoardUtils(plugin);
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
            Player player = (Player) sender;
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
                    deathBoardUtils.showDeathBoard(player);
                    deathBoardUtils.setPlayerDeathBoardVisibility(player, true);
                    sender.sendMessage("§e[§lTempDeathBanner§l] §aOpened the death board.");
                    return true;

                case "closedeathboard":
                    // Close the player's death board
                    if (!sender.hasPermission("tempdeathbanner.closedeathboard")){
                        sender.sendMessage("§e[§lTempDeathBanner§l] §cYou don't have permission to close deathboard.");
                        return true;
                    }
                    deathBoardUtils.setPlayerDeathBoardVisibility(player, false);
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
}
