package me.flyingtaco725.tempDeathBanner;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;

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
    public String messageContainer;
    public String banLengthContainer;
    public String graceMessageContainer;
    public int maxBanLength;
    public int grace;
    public String scoreBoardTitle;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Override
    public void onEnable() {

        // Registering the commands with the "tdb" prefix and a space
        getCommand("tdb").setExecutor(this);

        // create config
        saveDefaultConfig();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // set increment value
        increment = getConfig().getInt("increment");
        multiplier = getConfig().getInt("multiplier");
        baseMulti = getConfig().getInt("baseMulti");
        incOrMulti = getConfig().getBoolean("incOrMulti");
        messageContainer = getConfig().getString("messageContainer");
        banLengthContainer = getConfig().getString("banLengthContainer");
        graceMessageContainer = getConfig().getString("graceMessageContainer");
        maxBanLength = getConfig().getInt("maxBanLength");
        grace = getConfig().getInt("grace");
        scoreBoardTitle = getConfig().getString("scoreBoardTitle");

        // Load player data from JSON file (if it exists)
        loadPlayerBanList();
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Check if the command executed is 'tdb'
        if (command.getName().equalsIgnoreCase("tdb")) {
            // Check if the sender has permission to use the plugin
            if (!sender.hasPermission("tempdeathbanner.use")) {
                sender.sendMessage("You don't have permission to use this command.");
                return true;
            }

            // Check if any arguments are provided
            if (args.length == 0) {
                sender.sendMessage("Usage: /tdb [resetdeaths|resetdeathsall|showdeathboard|closedeathboard]");
                return true;
            }

            // Process the sub-commands
            switch (args[0].toLowerCase()) {
                case "resetdeaths":
                    // Check if the sender has permission to reset deaths for a specific player
                    if (!sender.hasPermission("tempdeathbanner.resetdeaths")) {
                        sender.sendMessage("§e[§lTempDeathBanner§l] §cYou don't have permission to reset deaths for a specific player.");
                        return true;
                    }

                    // Check if the player name argument is provided
                    if (args.length < 2) {
                        sender.sendMessage("Usage: /tdb resetdeaths <playerName>");
                        return true;
                    }

                    // Call the function to reset deaths for a specific player
                    resetDeaths(sender, args[1]);
                    return true;

                case "resetdeathsall":
                    // Check if the sender has permission to reset deaths for all players
                    if (!sender.hasPermission("tempdeathbanner.resetdeathsall")) {
                        sender.sendMessage("§e[§lTempDeathBanner§l] §cYou don't have permission to reset deaths for all players.");
                        return true;
                    }

                    // Call the function to reset deaths for all players
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
                    sender.sendMessage("Usage: /tdb [resetdeaths|resetdeathsall]");
                    return true;
            }
        }
        return false;
    }

    public void showDeathBoard(CommandSender sender)
    {
        ScoreboardManager manager;
        Scoreboard deathboard;
        Objective objective;
        Player p = (Player) sender;

        // scoreboard create
        manager = Bukkit.getScoreboardManager();
        deathboard = manager.getNewScoreboard();
        objective = deathboard.registerNewObjective("deathboard", "dummy", scoreBoardTitle);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        loadDeathBoard(manager, deathboard, objective, p);
    }

    public void loadDeathBoard(ScoreboardManager manager, Scoreboard deathboard, Objective objective, Player p){
        // Sort the players based on death count and then by name if there's a tie
        Collections.sort(PlayerBanList, new Comparator<MCPlayer>() {
            @Override
            public int compare(MCPlayer player1, MCPlayer player2) {
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
        int numPlayers = Math.min(5, PlayerBanList.size());
        for (int i = 0; i < numPlayers; i++) {
            MCPlayer player = PlayerBanList.get(i);
            String playerName = player.getPlayerName();
            int deathCount = player.getDeathCount();

            // Set the score for the player in the scoreboard
            Score score = objective.getScore("§a" + playerName);
            score.setScore(deathCount);
        }

        p.setScoreboard(deathboard);

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
    /   this function verifies that the player is online before resetting a specific players
    /   death counter
    */
    private void resetDeaths(CommandSender sender, String playerName){
        // Get the player's UUID
        Player player = Bukkit.getPlayer(playerName);

        if (player != null){
            // player exists and is online, get unique ID
            UUID uuid = player.getUniqueId();
            resetPlayerDeathCounter(player);
            player.sendMessage("§e[§lTempDeathBanner§l] §c" + playerName + "'s §adeath count has been set back to §c0");
            savePlayerBanList();
        }
        else
        {
            // Player is offline or does not exist
            sender.sendMessage("§e[§lTempDeathBanner§l] §aPlayer with name §c" + playerName + " §ais offline or does not exist.");
        }

    }
    /*
    /   This function loops through the death tracking array and resets the death counter
    /   for a specified player
    */
    private void resetPlayerDeathCounter(Player playerOBJ)
    {
        for (MCPlayer player : PlayerBanList) {
            if (player.getPlayerUUID().equals(playerOBJ.getUniqueId())) {
                player.setDeathCount(0);
                player.setLastMilli(0);
            }
        }
    }
    /*
    /   This function loops through the death tracking array and resets the death counter
    /   for ALL players
    */
    private void resetDeathsAll(CommandSender sender){
        for (MCPlayer player : PlayerBanList){
            player.setDeathCount(0);
            player.setLastMilli(0);
        }
        savePlayerBanList();
        getServer().broadcastMessage("§e[§lTempDeathBanner§l] §cAll Players §ahave had their death count returned to §c0");
    }
    /*
    /   This function handles everything involved in banning a player upon their death
    /   and handles inventory drops
    */
    private void banHandler(Player playerOBJ) {
        for (MCPlayer player : PlayerBanList) {
            if (player.getPlayerUUID().equals(playerOBJ.getUniqueId())) {
                // Increment death count
                player.setDeathCount(player.getDeathCount() + 1);

                // calculate ban time
                int banLength = deathCalc(incOrMulti, player, multiplier);

                // if banLength exceeds the maxBanLength simply set it to max ban length
                if (maxBanLength != -1 && banLength > maxBanLength)
                {
                    banLength = maxBanLength;
                }

                if (banLength != 0)
                {
                    // acquire players inventory so we can force drop their items
                    Inventory inventory = playerOBJ.getInventory();

                    // the ban command im using causes keep inventory, so we must do this to force drop all the players items
                    inventory.clear();

                    // convert the banLength into a date time
                    Date banExpiration = convertBanLengthToExpDate(banLength);

                    // change millisecond time to a non-infuriating format to read
                    String banInWords = convertBanLengthToWords(banLength);

                    // Issue ban
                    BanList banList = Bukkit.getBanList(BanList.Type.NAME);
                    banList.addBan(player.getPlayerName(), "You were banned for: " + banInWords, banExpiration, "Server");

                    // Kick Player
                    playerOBJ.kickPlayer("For your " + ordinal(player.getDeathCount()) + " death, you have been banned for a length of " + banInWords);

                    // Broadcast the death notification message
                    String gracesRemaining = "0";
                    getServer().broadcastMessage(messageModifier(messageContainer, player.getPlayerName(), player.getDeathCount(), banInWords, gracesRemaining));
                    getServer().broadcastMessage(messageModifier(banLengthContainer, player.getPlayerName(), player.getDeathCount(), banInWords, gracesRemaining));
                } else{
                    // get amount of deaths remaining before a players ban
                    String gracesRemaining = String.valueOf((grace - player.getDeathCount()));
                    getServer().broadcastMessage(messageModifier(graceMessageContainer, player.getPlayerName(), player.getDeathCount(), "0 Seconds", gracesRemaining));
                }
            }
        }
    }

    /*
    /   This function modifies the various broadcast
    /   messages involved with the ban event
     */
    public String messageModifier(String messageContainer, String playerName, int playerDeathCount, String banInWords, String gracesRemaining) {
        if(messageContainer.contains("[player]")) {
            messageContainer = messageContainer.replace("[player]",  playerName);
        }
        if(messageContainer.contains("[dc]")) {
            messageContainer = messageContainer.replace("[dc]", String.valueOf(playerDeathCount));
        }
        if(messageContainer.contains("[dcth]")) {
            messageContainer = messageContainer.replace("[dcth]",  ordinal(playerDeathCount));
        }
        if(messageContainer.contains("[bl]")) {
            messageContainer = messageContainer.replace("[bl]", banInWords);
        }
        if(messageContainer.contains("[gr]")) {
            messageContainer = messageContainer.replace("[gr]", gracesRemaining);
        }
        return messageContainer;
    };
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
    private int deathCalc(boolean incOrMulti, MCPlayer player, int multiplier) {

        int banLength;

        // if grace deaths == 0, no grace deaths, ban handler functions as normal
        if (grace == 0) {
            if (incOrMulti) {
                // increment

                // all deaths for increment are handled the same
                banLength = player.getDeathCount() * increment;
                savePlayerBanList();
                return banLength;
            } else {
                // multiply

                if (player.getDeathCount() == 1) {
                    // handle first death differently
                    banLength = baseMulti;
                    player.setLastMilli(banLength);
                    savePlayerBanList();
                    return banLength;
                } else {
                    // handle all following deaths the same
                    banLength = multiplier * player.getLastMilli();
                    player.setLastMilli(banLength);
                    savePlayerBanList();
                    return banLength;
                }
            }
        }
        else
        {
            if (grace >= player.getDeathCount()) {
                // this is a free death
                return 0;
            }
            else {
                int adjustedDeathCount = player.getDeathCount() - grace;
                // this isn't a free death
                if (incOrMulti) {
                    // increment

                    // all deaths for increment are handled the same
                    banLength = adjustedDeathCount * increment;
                    savePlayerBanList();
                    return banLength;
                } else {
                    // multiply
                    if (player.getDeathCount() == grace + 1) {

                        // handle first death differently
                        banLength = baseMulti;
                        player.setLastMilli(banLength);
                        savePlayerBanList();
                        return banLength;
                    } else {

                        // handle all following deaths the same
                        banLength = multiplier * player.getLastMilli();
                        player.setLastMilli(banLength);
                        savePlayerBanList();
                        return banLength;
                    }
                }
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
            System.out.println("Player data saved successfully.");
        } catch (IOException e) {
            System.out.println("Failed to save player data: " + e.getMessage());
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
                    System.out.println("Player data loaded successfully.");
                }
            } catch (IOException e) {
                System.out.println("Failed to load player data: " + e.getMessage());
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