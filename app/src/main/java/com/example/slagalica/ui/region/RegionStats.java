package com.example.slagalica.ui.region;

public class RegionStats {
    public final String region;
    public final int totalStars;
    public final int playerCount;
    public final int activePlayers;
    public final int goldMedals;
    public final int silverMedals;
    public final int bronzeMedals;
    public int rank;

    public RegionStats(String region, int totalStars, int playerCount, int activePlayers,
                       int goldMedals, int silverMedals, int bronzeMedals) {
        this.region = region;
        this.totalStars = totalStars;
        this.playerCount = playerCount;
        this.activePlayers = activePlayers;
        this.goldMedals = goldMedals;
        this.silverMedals = silverMedals;
        this.bronzeMedals = bronzeMedals;
    }
}