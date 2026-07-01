package com.example.slagalica.domain.service;
public class LeagueService {

    private static final int[] THRESHOLDS = {
            100,   // Liga 1
            200,   // Liga 2
            400,   // Liga 3
            800,   // Liga 4
            1600   // Liga 5
    };

    private static final String[] LEAGUE_NAMES = {
            "Početna", "Bronzana", "Srebrna", "Zlatna", "Platinasta", "Dijamantska"
    };

    public static class LeagueResult {
        public final int newLeague;
        public final boolean promoted;
        public final boolean relegated;

        public LeagueResult(int newLeague, boolean promoted, boolean relegated) {
            this.newLeague = newLeague;
            this.promoted = promoted;
            this.relegated = relegated;
        }
    }

    public LeagueResult calculateLeague(int stars, int currentLeague) {
        int newLeague = 0;
        for (int i = THRESHOLDS.length - 1; i >= 0; i--) {
            if (stars >= THRESHOLDS[i]) {
                newLeague = i + 1;
                break;
            }
        }

        boolean promoted = newLeague > currentLeague;
        boolean relegated = newLeague < currentLeague;

        return new LeagueResult(newLeague, promoted, relegated);
    }

    public int getDailyTokenBonus(int league) {
        return 5 + league;
    }

    public static String getLeagueName(int league) {
        if (league >= 0 && league < LEAGUE_NAMES.length) return LEAGUE_NAMES[league];
        return "Početna";
    }

    public static int getLeagueThreshold(int league) {
        if (league <= 0) return 0;
        if (league - 1 < THRESHOLDS.length) return THRESHOLDS[league - 1];
        return Integer.MAX_VALUE;
    }
}