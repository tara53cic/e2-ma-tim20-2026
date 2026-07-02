package com.example.slagalica.domain.service;

import com.example.slagalica.data.MatchRepository;


public class ConnectingGameHelper {

    private final MatchRepository matchRepository;
    private final GameFlowManager gameFlowManager;

    public ConnectingGameHelper() {
        this.matchRepository = new MatchRepository();
        this.gameFlowManager = new GameFlowManager();
    }


    public boolean shouldSkipOpponentTimer(String matchId) {

        return matchRepository.getMatch(matchId)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Boolean p1TimeOut = task.getResult().getBoolean("player1_timedOut");
                        Boolean p2TimeOut = task.getResult().getBoolean("player2_timedOut");
                        return Boolean.TRUE.equals(p1TimeOut) || Boolean.TRUE.equals(p2TimeOut);
                    }
                    return false;
                }).getResult();
    }


    public int getAdjustedTimerDuration(String matchId, int defaultDuration) {

        if (shouldSkipOpponentTimer(matchId)) {
            return 0;
        }
        return defaultDuration;
    }


    public void proceedToNextPlayer(String matchId, boolean currentPlayerIsPlayer1) {

    }


    public void checkAndHandleTimeout(String matchId, String playerId, boolean isPlayer1, long timeoutThresholdMs) {
        matchRepository.getMatch(matchId).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Long createdAt = doc.getLong("createdAt");
                if (createdAt != null) {
                    long elapsedTime = System.currentTimeMillis() - createdAt;
                    if (elapsedTime > timeoutThresholdMs) {
                        gameFlowManager.handlePlayerTimeout(matchId, isPlayer1);
                    }
                }
            }
        });
    }


    public long getMinimumWaitForAbsentOpponent() {
        return 0; // No waiting, process immediately
    }
}

