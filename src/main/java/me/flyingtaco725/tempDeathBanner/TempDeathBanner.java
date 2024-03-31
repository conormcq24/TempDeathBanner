package me.flyingtaco725.tempDeathBanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class TempDeathBanner extends JavaPlugin implements Listener {

    // List of player Info Objects
    public List<MCPlayer> PlayerBanList = new ArrayList<>();
    // Amount of milliseconds player bans will increment by if increment is selected in config (config var)
    public int increment;
    // How much we will multiply last ban length by when multiplier is selected (config var)
    public int multiplier;
    // initial ban length in milliseconds when multiply is selected in config (config var)
    public int baseMulti;
    // true for increment, false for multiply (config var)
    public boolean incOrMulti;
    // value of death broadcast message
    public String messagePartOne;
    public String messagePartTwo;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onEnable() {

        // create config
        saveDefaultConfig();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // set increment value
        increment = getConfig().getInt("increment");
        multiplier = getConfig().getInt("multiplier");
        baseMulti = getConfig().getInt("baseMulti");
        incOrMulti = getConfig().getBoolean("incOrMulti");
        messagePartOne = getConfig().getString("messagePartOne");
        messagePartTwo = getConfig().getString("messagePartTwo");

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
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Get player's UUID
        UUID playerUUID = event.getEntity().getUniqueId();
        // Call banHandler
        banHandler(event.getEntity());
    }

    /*
    /   This function handles everything involved in banning a player upon their death
    /   and handles inventory drops
    */
    private void banHandler(Player playerOBJ) {
        for (MCPlayer player : PlayerBanList) {
            if (player.getPlayerUUID().equals(playerOBJ.getUniqueId())) {

                // acquire players inventory so we can force drop their items
                Inventory inventory = playerOBJ.getInventory();

                // the ban command im using causes keep inventory, so we must do this to force drop all the players items
                inventory.clear();

                // Increment death count
                player.setDeathCount(player.getDeathCount() + 1);

                // calculate ban time
                int banLength = deathCalc(incOrMulti, player, multiplier);

                // convert the banLength into a date time
                Date banExpiration = convertBanLengthToExpDate(banLength);

                // change millisecond time to a non-infuriating format to read
                String banInWords = convertBanLengthToWords(banLength);

                // Issue ban
                BanList banList = Bukkit.getBanList(BanList.Type.NAME);
                banList.addBan(player.getPlayerName(), "You were banned for: " + banInWords, banExpiration, "Server");

                // Kick Player
                playerOBJ.kickPlayer("For your " + ordinal(player.getDeathCount()) + " death, you have been banned for a length of " + banInWords);

                // Broadcast the message
                getServer().broadcastMessage("§e[§lTempDeathBanner§l] §c" + player.getPlayerName() + "§a"+ messagePartOne + "§c§l" + ordinal(player.getDeathCount()) + "§l§a" + messagePartTwo);
                getServer().broadcastMessage("§eBan Length: §l§c" + banInWords);
            }
        }
    }

    /*
    /   This function takes the milliseconds of banLength and converts it into a sentence to be used in server broadcast
    /   and ban messages.
    */
    public String convertBanLengthToWords(int banLength){
        long days = banLength / (1000 * 60 * 60 * 24);
        long hours = (banLength % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = Math.round((double) (banLength % (1000 * 60 * 60)) / (1000 * 60));

        // Build the string
        StringBuilder stringBuilder = new StringBuilder();
        if (days > 1) {
            stringBuilder.append(days).append(" days ");
        } else if (days == 1){
            stringBuilder.append(days).append(" day ");
        }
        if (hours > 1) {
            stringBuilder.append(hours).append(" hours ");
        } else if (hours == 1){
            stringBuilder.append(hours).append(" hour ");
        }
        if (minutes > 1) {
            stringBuilder.append(minutes).append(" minutes");
        } else if (minutes == 1){
            stringBuilder.append(minutes).append(" minute");
        }

        return stringBuilder.toString();
    }
    /*
    /   This function takes the milliseconds of banLength and adds it to the current datetime, in order to formulate when a player should be unbanned
    /   it then returns that datetime to be used in the ban command
    */
    public Date convertBanLengthToExpDate (int banLength)
    {
        long currentTimeMillis = System.currentTimeMillis();
        long futureTimeMillis = currentTimeMillis + banLength;
        Date futureDate = new Date(futureTimeMillis);
        return futureDate;
    }

    /*
    /   This function performs the calculation to determine just how long a players ban should be in one of two ways
    /   depending on variables assigned via the plugins config file
    */
    private int deathCalc(boolean incOrMulti, MCPlayer player, int multiplier)
    {
        int banLength;

        if (incOrMulti)
        {
            // increment

            // death 1: (1 * 3600000 = 3600000 milliseconds = 1 hour)
            // death 2: (2 * 3600000 = 7200000 milliseconds = 2 hours)
            // death 3: (3 * 3600000 = 10800000 milliseconds = 3 hours)
            banLength = player.getDeathCount() * increment;
            return banLength;
        }
        else
        {
            // multiply
            if (player.getDeathCount() == 1)
            {
                // 1st Death and Later
                // death 1: (baseMulti(3600000) = 3600000 milliseconds = 1 hour)
                banLength = baseMulti;
                player.setLastMilli(banLength);
                return banLength;
            }
            else
            {
                // 2nd Death and Later
                // death 2: (multiplier(2) * lastMilli(3600000) = 7200000 milliseconds = 2 hours)
                // death 3: (multiplier(2) * lastMilli(7200000) = 14400000 milliseconds = 4 hours)
                banLength = multiplier * player.getLastMilli();
                player.setLastMilli(banLength);
                return banLength;
            }
        }
    }


    /*
     /   This function checks if a player already exists within the death tracking array
     /   returns true if they do, false if they don't
     */
    private boolean isPlayerInArray(UUID playerUUID) {
        return PlayerBanList.stream().anyMatch(player -> player.getPlayerUUID().equals(playerUUID));
    }

    /*
    /   This function saves the death tracking array to persistent data, this allows us to keep track of
    /   how many times a player has died even after a server restart
    */
    private void savePlayerBanList() {
        // Save the PlayerBanList to a file within the "plugins/TempDeathFolder" directory
        File playerDataFile = new File(getDataFolder(), "player_data.json");
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

    /*
    /   This function loads the death tracking array on server startup, from persistent data
    /   allowing us to preserve player death count and other information after server stops/restarts
    */
    private void loadPlayerBanList() {
        // Load the PlayerBanList from a file within the "plugins/TempDeathFolder" directory
        File playerDataFile = new File(getDataFolder(), "player_data.json");
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

    /*
    /   This function converts numbers to spoken version of the number
    /   ex: 1 -> 1st, 2 -> 2nd etc
    */
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