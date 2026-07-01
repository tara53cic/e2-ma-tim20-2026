package com.example.slagalica.domain.service;

public class UserStatsService {

    public static class UserStatsResult {
        public final boolean isWinner;
        public final int totalStarsChange;
        public final int newStars;
        public final int newTokens;
        public final int tokensAdded;

        public UserStatsResult(boolean isWinner, int totalStarsChange,
                               int newStars, int newTokens, int tokensAdded) {
            this.isWinner = isWinner;
            this.totalStarsChange = totalStarsChange;
            this.newStars = newStars;
            this.newTokens = newTokens;
            this.tokensAdded = tokensAdded;
        }
    }

    public UserStatsResult calculateNewStats(int myScore, int opponentScore,
                                             int currentStars, int currentTokens) {
        boolean isWinner = myScore >= opponentScore;

        int baseStars = isWinner ? 10 : -10;
        int bonusStars = myScore / 40;
        int totalStarsChange = baseStars + bonusStars;

        int newStars = currentStars + totalStarsChange;
        if (newStars < 0) newStars = 0;

        int tokensPrevious = currentStars / 50;
        int tokensNow      = newStars / 50;
        int tokensAdded    = Math.max(0, tokensNow - tokensPrevious);
        int newTokens      = currentTokens + tokensAdded;

        return new UserStatsResult(isWinner, totalStarsChange, newStars, newTokens, tokensAdded);
    }
}