package me.flyingtaco725.tempDeathBanner.EventHandlers;

import me.flyingtaco725.tempDeathBanner.PlayerInfo.PlayerInfo;
import me.flyingtaco725.tempDeathBanner.tempDeathBanner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

import static org.bukkit.Bukkit.getLogger;

/* This Class detects player join events and handles them */
public class PlayerJoinHandler implements Listener {
    private final tempDeathBanner plugin;

    public PlayerJoinHandler(tempDeathBanner plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        UUID playerUUID = event.getPlayer().getUniqueId();

        boolean doesPlayerExist = false;

        if (plugin.banManagementList != null) {
            doesPlayerExist = plugin.banManagementList.stream().anyMatch(player -> player.getPlayerUUID().equals(playerUUID));
        }

        if (!doesPlayerExist) {
            plugin.banManagementList.add(new PlayerInfo(playerName, playerUUID, 0));
            getLogger().info(playerName + " has entered the server, added to death tracking array ");
        }
    }
}
