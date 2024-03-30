package me.flyingtaco725.tempDeathBanner;
import java.util.UUID;

public class MCPlayer {
    private String playerName;
    private UUID playerUUID;
    private int deathCount;


    public MCPlayer(String playerName, UUID playerUUID, int deathCount){
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.deathCount = deathCount;
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

}
