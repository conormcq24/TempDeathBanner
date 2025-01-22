package me.flyingtaco725.tempdeathbanner_revamped.EventHandlers;

import me.flyingtaco725.tempdeathbanner_revamped.PlayerInfo.PlayerInfo;
import me.flyingtaco725.tempdeathbanner_revamped.TempDeathBanner_Revamped;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;

import java.util.Date;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

/* This Class detects player death events and handles them */
public class PlayerDeathHandler implements Listener {
        private final TempDeathBanner_Revamped plugin;
        public PlayerDeathHandler(TempDeathBanner_Revamped plugin) {
            this.plugin= plugin;
        }

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
          String playerName = event.getEntity().getName();
          UUID playerUUID = event.getEntity().getUniqueId();
          banLogic(event.getEntity());
        }

        /*
            FUNCTION: banLogic()
            PURPOSE: this functions purpose is to handle the core logic that occurs once a player dies,
                     it will refer to the banManagementList, and check for a match on the dying players
                     unique ID. if it finds a match it will then increment the players death count, calculate
                     their ban length, check for a set maximum ban length, and prevent ban lengths from going
                     over it if it exists. it will then process the ban and notify the server with an appropriate
                     death message
         */
        private void banLogic(Player theDeadGuy){
            for (PlayerInfo player : plugin.banManagementList){
                if(player.getPlayerUUID().equals(theDeadGuy.getUniqueId())) {
                    player.setDeathCount(player.getDeathCount() + 1 );
                    int banLength = calculateBanLength(player);
                    if (plugin.maximumBanLength != -1 && banLength > plugin.maximumBanLength){
                        banLength = plugin.maximumBanLength;
                    }
                    // logic for when a ban is applied
                    if (banLength != 0){
                        // the ban command im using causes keep inventory on death, so we must do this to force drop all the players items
                        Inventory inventory = theDeadGuy.getInventory();
                        inventory.clear();

                        Date banExpiration = convertBanLengthToExpDate(banLength);
                        String banInWords = convertBanLengthToWords(banLength);
                        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
                        banList.addBan(player.getPlayerName(), "You were banned for: " + banInWords, banExpiration, "Server");
                        theDeadGuy.kickPlayer("For your " + ordinal(player.getDeathCount()) + " death, you have been banned for a length of " + banInWords);
                        getServer().broadcastMessage(messageModifier(plugin.messageContainer, player.getPlayerName(), player.getDeathCount(), banInWords, "0"));
                        getServer().broadcastMessage(messageModifier(plugin.banLengthContainer, player.getPlayerName(), player.getDeathCount(), banInWords, "0"));
                    }
                    // logic for when they die with a grace live remaining
                    else{
                        String gracesRemaining = String.valueOf((plugin.graceLives - player.getDeathCount()));
                        getServer().broadcastMessage(messageModifier(plugin.graceMessageContainer, player.getPlayerName(), player.getDeathCount(), "0 Seconds", gracesRemaining));
                    }
                }
            }
        }



        /*
            FUNCTION: calculateBanLength()
            PURPOSE: this function takes into account configuration settings and player history, and determines
                     the proper penalty for the specific player upon their death. This is probably the most confusing
                     section of code in the plugin, I would like to eventually simplify it so that it is not an ugly
                     giant nested if statement.
         */
        private int calculateBanLength(PlayerInfo player){
            // the length the dying player will be banned in milliseconds
            int banLength;
            // if the user has set grace lives to be 0, it means that all deaths are punished
            if (plugin.graceLives == 0){
                // if incOrMultiply is true it means that the user has selected increment as the ban punishment
                if(plugin.incOrMultiply){
                    // if the user selected increment we just multiply the death count by the incrementByXMilliseconds for ban length
                    banLength = player.getDeathCount() * plugin.incrementByXMilliseconds;
                    // save banManagementList to json
                    plugin.banManager.saveList(plugin.playerDataFile, plugin.banManagementList);
                    return banLength;
                }
                // if incOrMultiply is false it means that the user has selected multiply as the ban punishment
                else{
                    // handle first death in multiplier mode
                    if(player.getDeathCount() == 1){
                        // the first death in multiplier is just the initialBanLength
                        banLength = plugin.initialBanLength;
                        player.setLastMilli(banLength);
                        // save banManagementList to json
                        plugin.banManager.saveList(plugin.playerDataFile, plugin.banManagementList);
                        return banLength;
                    }
                    // handle the rest of the deaths in multiplier mode
                    else {
                        // all other deaths in multiplier mode we multiply the users last ban length in milliseconds by the multiplier
                        banLength = plugin.multiplier * player.getLastMilli();
                        player.setLastMilli(banLength);
                        // save banManagementList to json
                        plugin.banManager.saveList(plugin.playerDataFile, plugin.banManagementList);
                        return banLength;
                    }
                }
            }
            // if the user has set a certain amount of grace lives we do nothing if the users death count is less than grace lives
            else {
                // free death
                if(plugin.graceLives >= player.getDeathCount()) {
                    // save list so that the incremented death count gets saved to json, but no ban
                    plugin.banManager.saveList(plugin.playerDataFile, plugin.banManagementList);
                    return 0;
                }
                //  this isn't a free death
                else {
                    // for grace lives we have to offset the death count by the allotted grace lives for calcuating ban time
                    int adjustedDeathCount = player.getDeathCount() - plugin.graceLives;
                    // if incOrMultiply is true it means that the user has selected increment as the ban punishment
                    if(plugin.incOrMultiply){
                        // if the user selected increment we just multiply the death count by the incrementByXMilliseconds for ban length
                        banLength = adjustedDeathCount * plugin.incrementByXMilliseconds;
                        // save banManagementList to json
                        plugin.banManager.saveList(plugin.playerDataFile, plugin.banManagementList);
                        return banLength;
                    }
                    // if incOrMultiply is false it means that the user has selected multiply as the ban punishment
                    {
                        // if were in grace mode and have reached the first death that is punishable
                        if(player.getDeathCount() == plugin.graceLives + 1){
                            // first punishable death is just the configuration initial ban length
                            banLength = plugin.initialBanLength;
                            player.setLastMilli(banLength);
                            // save banManagementList to json
                            plugin.banManager.saveList(plugin.playerDataFile, plugin.banManagementList);
                            return banLength;
                        }
                        // if we're in grace mode and have reach any other punishable death
                        else{
                            banLength = plugin.multiplier * player.getLastMilli();
                            player.setLastMilli(banLength);
                            // save banManagementList to json
                            plugin.banManager.saveList(plugin.playerDataFile, plugin.banManagementList);
                            return banLength;
                        }
                    }
                }
            }
        }


        /*
            FUNCTION: convertBanLengthToExpDate()
            PURPOSE: takes the ban length in milliseconds and adds it to the current date and time,
                     returns dateTime to be used in ban command
         */
        private Date convertBanLengthToExpDate (int banLength)
        {
            long currentTimeMillis = System.currentTimeMillis();
            long futureTimeMillis = currentTimeMillis + banLength;
            Date futureDate = new Date(futureTimeMillis);
            return futureDate;
        }


        /*
            FUNCTION: convertBanLengthToWords()
            PURPOSE: takes the ban length in milliseconds and converts it to a sentence to be used by
                     ban messages
         */
        private String convertBanLengthToWords(int banLength){
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
            FUNCTION: ordinal()
            PURPOSE: converts digits into their ordinal form
         */
        private static String ordinal(int i) {
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

        /*
            FUNCTION: messageModifier()
            PURPOSE: this function handles the custom formatting I outlined in the config.yml file
                     allowing the server admins more control over the messages that are displayed
                     when a player dies
         */
        private String messageModifier(String messageContainer, String playerName, int playerDeathCount, String banInWords, String gracesRemaining) {
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
        }
}
