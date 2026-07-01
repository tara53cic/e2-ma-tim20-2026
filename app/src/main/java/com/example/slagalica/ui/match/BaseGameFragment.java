package com.example.slagalica.ui.match;

import androidx.fragment.app.Fragment;
import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.domain.service.GameStateMonitor;
import com.google.firebase.auth.FirebaseAuth;


public abstract class BaseGameFragment extends Fragment {

    protected String matchId;
    protected String currentUserId;
    protected String opponentUserId;
    protected MatchRepository matchRepository;
    protected GameStateMonitor gameStateMonitor;
    private boolean isPlayer1;

    private java.util.Timer heartbeatTimer;
    private boolean gameActive = true;

    protected void initializeGameMonitoring(String matchId, boolean isPlayer1) {
        this.matchId = matchId;
        this.isPlayer1 = isPlayer1;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.matchRepository = new MatchRepository();
        this.gameStateMonitor = new GameStateMonitor();


        if (matchId != null) {
            matchRepository.getMatch(matchId)
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            this.opponentUserId = isPlayer1 ?
                                    doc.getString("player2_id") :
                                    doc.getString("player1_id");

                            startAbandonmentMonitoring();
                        }
                    });
        }
    }


    private void startAbandonmentMonitoring() {
        if (heartbeatTimer != null) heartbeatTimer.cancel();

        heartbeatTimer = new java.util.Timer();
        heartbeatTimer.scheduleAtFixedRate(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        if (!gameActive) {
                            this.cancel();
                            return;
                        }

                        gameStateMonitor.isPlayerActive(opponentUserId)
                                .addOnSuccessListener(isActive -> {
                                    if (!isActive && gameActive) {
                                        handleOpponentAbandonment();
                                    }
                                });
                    }
                },
                2000,
                3000
        );
    }


    protected void handlePlayerTimeout(int roundNumber) {
        if (!gameActive) return;

        gameStateMonitor.handlePlayerTimeout(matchId, isPlayer1)
                .addOnSuccessListener(v -> {

                    onTimeoutOccurred(roundNumber);

                });
    }


    private void handleOpponentAbandonment() {
        if (!gameActive) return;

        gameActive = false;
        if (heartbeatTimer != null) heartbeatTimer.cancel();

        gameStateMonitor.detectAndHandleAbandonment(matchId, opponentUserId, currentUserId)
                .addOnSuccessListener(v -> {
                    onOpponentAbandoned();
                });
    }


    @Override
    public void onPause() {
        super.onPause();
        if (gameActive && matchId != null) {
            // Igrač je napustio igru
            gameActive = false;
            if (heartbeatTimer != null) heartbeatTimer.cancel();

            handlePlayerAbandonedTheGame();
        }
    }


    private void handlePlayerAbandonedTheGame() {
        gameStateMonitor.detectAndHandleAbandonment(matchId, currentUserId, opponentUserId)
                .addOnSuccessListener(v -> {

                    onPlayerAbandoned();
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gameActive = false;
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }


    protected void onOpponentAbandoned() {

    }


    protected void onPlayerAbandoned() {

    }


    protected void onTimeoutOccurred(int roundNumber) {

    }


    protected void endGameSession() {
        gameActive = false;
        if (heartbeatTimer != null) heartbeatTimer.cancel();
    }
}

