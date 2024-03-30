package me.flyingtaco725.tempDeathBanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class TempDeathBanner extends JavaPlugin implements Listener {

    // Global variables
    public List<MCPlayer> PlayerBanList = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onEnable() {
        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);
        // Load player data from JSON file (if it exists)
        loadPlayerBanList();
    }

    // When server shuts down, save the array to persistent data
    @Override
    public void onDisable() {
        savePlayerBanList();
    }

    // When player joins server, send a welcome message
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Get player's relevant info on join
        String playerName = event.getPlayer().getName();
        UUID playerUUID = event.getPlayer().getUniqueId();

        // Check if they already exist within the array
        boolean doesPlayerExist = isPlayerInArray(playerUUID);

        // If they don't, store fresh information to array
        if (!doesPlayerExist) {
            PlayerBanList.add(new MCPlayer(playerName, playerUUID, 0));
            getLogger().info(playerName + " has entered the server, added to death tracking array ");
        }
    }

    // When player respawns, handle it
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Get player's UUID
        UUID playerUUID = event.getPlayer().getUniqueId();
        // Call banHandler
        banHandler(event.getPlayer());
    }

    // Ban handler method
    private void banHandler(Player playerOBJ) {
        for (MCPlayer player : PlayerBanList) {
            if (player.getPlayerUUID().equals(playerOBJ.getUniqueId())) {
                // Increment death count
                player.setDeathCount(player.getDeathCount() + 1);
                // Issue ban
                BanList banList = Bukkit.getBanList(BanList.Type.NAME);
                banList.addBan(player.getPlayerName(), "You were banned for your " + ordinal(player.getDeathCount()) + " death, you will be unbanned in x minutes", (Date) null, "Server");
                // Kick Player
                playerOBJ.kickPlayer("You were banned for your " + ordinal(player.getDeathCount()) + " death, Earning a ban time of x minutes");
                // Broadcast the message
                getServer().broadcastMessage(player.getPlayerName() + " has died for the " + ordinal(player.getDeathCount()) + " time");
            }
        }
    }

    // Check if player exists in the array
    private boolean isPlayerInArray(UUID playerUUID) {
        return PlayerBanList.stream().anyMatch(player -> player.getPlayerUUID().equals(playerUUID));
    }

    // Save PlayerBanList to a JSON file
    private void savePlayerBanList() {
        File dataFolder = getDataFolder();
        // Create a folder named "TempDeathBanner" within the plugin's data folder
        File tempDeathBannerFolder = new File(dataFolder, "TempDeathBanner");
        if (!tempDeathBannerFolder.exists()) {
            tempDeathBannerFolder.mkdirs(); // Creates directories including any missing parent directories
        }

        // Save the PlayerBanList to a file within the "TempDeathBanner" folder
        File playerDataFile = new File(tempDeathBannerFolder, "player_data.json");
        try (Writer writer = new FileWriter(playerDataFile)) {
            // Convert the PlayerBanList to JSON string
            String json = gson.toJson(PlayerBanList);
            // Write JSON string to file
            writer.write(json);
            getLogger().info("Player data saved successfully.");
        } catch (IOException e) {
            getLogger().warning("Failed to save player data: " + e.getMessage());
        }
    }
    
    private void loadPlayerBanList() {
        File dataFolder = getDataFolder();
        // Create a folder named "TempDeathBanner" within the plugin's data folder
        File tempDeathBannerFolder = new File(dataFolder, "TempDeathBanner");
        if (!tempDeathBannerFolder.exists()) {
            return; // Folder doesn't exist, no data to load
        }

        // Load the PlayerBanList from a file within the "TempDeathBanner" folder
        File playerDataFile = new File(tempDeathBannerFolder, "player_data.json");
        if (playerDataFile.exists()) {
            try (Reader reader = new FileReader(playerDataFile)) {
                // Read JSON string from file
                Type type = new TypeToken<List<MCPlayer>>(){}.getType();
                List<MCPlayer> loadedList = gson.fromJson(reader, type);

                if (loadedList != null) {
                    PlayerBanList = loadedList;
                    getLogger().info("Player data loaded successfully.");
                }
            } catch (IOException e) {
                getLogger().warning("Failed to load player data: " + e.getMessage());
            }
        }
    }

    public static String ordinal(int i) {
        String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];

        }
    }
}