package me.flyingtaco725.tempDeathBanner.ListUtility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.flyingtaco725.tempDeathBanner.PlayerInfo.PlayerInfo;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BanManager {
    // this is required to convert java objects into json
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /*
        FUNCTION: loadList()
        PURPOSE: loads a json list container player death and tracking information
                 for banManagementList
     */
    public List<PlayerInfo> loadList(File playerDataFile) {
        List<PlayerInfo> loadedList = new ArrayList<>();  // Initialize as an empty list

        if (playerDataFile.exists()) {
            try (Reader reader = new FileReader(playerDataFile)) {
                Type type = new TypeToken<List<PlayerInfo>>(){}.getType();
                loadedList = gson.fromJson(reader, type);

                // If loadedList is still null, set it to an empty list
                if (loadedList == null) {
                    loadedList = new ArrayList<>();
                } else {
                    System.out.println("Player Data Loaded Successfully");
                }
            } catch (IOException e) {
                System.out.println("Failed to load player data: " + e.getMessage());
            }
        } else {
            System.out.println("Player data file does not exist, returning empty list.");
        }

        return loadedList;  // Always return an initialized (non-null) list
    }

    /*
        FUNCTION: saveList()
        PURPOSE: saves the java class called banManagementList which is a list of PlayerInfo objects into a json
                 file in the plugins folder, so that deaths and ban penalties can be called back after server shutoffs
    */
    public void saveList(File playerDataFile, List<PlayerInfo> banManagementList) {
        // Save the PlayerBanList to a file within the "plugins/TempDeathFolder" directory
        try (Writer writer = new FileWriter(playerDataFile)) {
            // Convert the PlayerBanList to JSON string
            String json = gson.toJson(banManagementList);
            // Write JSON string to file
            writer.write(json);
            System.out.println("Player data saved successfully.");
        } catch (IOException e) {
            System.out.println("Failed to save player data: " + e.getMessage());
        }
    }
}
