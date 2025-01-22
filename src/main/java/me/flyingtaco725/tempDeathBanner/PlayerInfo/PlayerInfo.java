package me.flyingtaco725.tempDeathBanner.PlayerInfo;

import java.util.UUID;

public class PlayerInfo {
    private String playerName;
    private UUID playerUUID;
    private int deathCount;
    private int lastMilli;


    public PlayerInfo(String playerName, UUID playerUUID, int deathCount){
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.deathCount = deathCount;
        // remember to set this to config variable later
        this.lastMilli = 0;
    }

    public void setPlayerName (String playerName){
        this.playerName = playerName;
    }

    public String getPlayerName (){
        return playerName;
    }

    public void setPlayerUUID (UUID playerUUID){
        this.playerUUID = playerUUID;
    }

    public UUID getPlayerUUID (){
        return playerUUID;
    }

    public void setDeathCount (int deathCount) {
        this.deathCount = deathCount;
    }

    public int getDeathCount(){
        return deathCount;
    }

    public void setLastMilli (int lastMilli){
        this.lastMilli = lastMilli;
    }

    public int getLastMilli(){
        return lastMilli;
    }
}
