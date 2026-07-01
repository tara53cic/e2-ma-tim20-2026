package com.example.slagalica.domain.service;


public class GameResultService {

    public static class GameResult {
        public final boolean isWinner;
        public final int myScore;
        public final int opponentScore;
        public final int starsChangeBase;
        public final int starsChangeBonus;
        public final int totalStarsChange;
        public final int newStars;
        public final int newTokens;
        public final int tokensGained;
        public final boolean isFriendly;
        public final boolean wasAbandonment;
        public final boolean wasTimeout;

        public GameResult(boolean isWinner, int myScore, int opponentScore,
                         int starsChangeBase, int starsChangeBonus, int totalStarsChange,
                         int newStars, int newTokens, int tokensGained,
                         boolean isFriendly, boolean wasAbandonment, boolean wasTimeout) {
            this.isWinner = isWinner;
            this.myScore = myScore;
            this.opponentScore = opponentScore;
            this.starsChangeBase = starsChangeBase;
            this.starsChangeBonus = starsChangeBonus;
            this.totalStarsChange = totalStarsChange;
            this.newStars = newStars;
            this.newTokens = newTokens;
            this.tokensGained = tokensGained;
            this.isFriendly = isFriendly;
            this.wasAbandonment = wasAbandonment;
            this.wasTimeout = wasTimeout;
        }
    }


    public GameResult calculateGameResult(int myScore, int opponentScore,
                                          int currentStars, int currentTokens,
                                          boolean isFriendly, boolean isAbandonment,
                                          boolean isTimeout) {
        // Friendly matches don't award/deduct stars
        if (isFriendly) {
            return new GameResult(
                    myScore >= opponentScore, myScore, opponentScore,
                    0, 0, 0,
                    currentStars, currentTokens, 0,
                    true, false, false
            );
        }


        int finalMyScore = isTimeout ? 0 : myScore;
        boolean isWinner = finalMyScore >= opponentScore;


        int starsChangeBase = isWinner ? 10 : -10;
        int starsChangeBonus = finalMyScore / 40;
        int totalStarsChange = starsChangeBase + starsChangeBonus;


        int newStars = currentStars + totalStarsChange;
        if (newStars < 0) newStars = 0;


        int tokensBefore = currentStars / 50;
        int tokensNow = newStars / 50;
        int tokensGained = Math.max(0, tokensNow - tokensBefore);
        int newTokens = currentTokens + tokensGained;

        return new GameResult(
                isWinner, finalMyScore, opponentScore,
                starsChangeBase, starsChangeBonus, totalStarsChange,
                newStars, newTokens, tokensGained,
                false, isAbandonment, isTimeout
        );
    }


    public boolean hasEnoughTokens(int currentTokens) {
        return currentTokens > 0;
    }


    public int getTokensNeededForGame() {
        return 1;
    }
}

