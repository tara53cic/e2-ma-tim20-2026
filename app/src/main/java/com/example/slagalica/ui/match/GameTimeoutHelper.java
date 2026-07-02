package com.example.slagalica.ui.match;

import android.os.CountDownTimer;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.slagalica.domain.service.GameStateMonitor;


public class GameTimeoutHelper {

    private final Fragment parentFragment;
    private final String matchId;
    private final boolean isPlayer1;
    private final GameStateMonitor gameStateMonitor;
    private CountDownTimer currentTimer;

    public GameTimeoutHelper(Fragment parent, String matchId, boolean isPlayer1) {
        this.parentFragment = parent;
        this.matchId = matchId;
        this.isPlayer1 = isPlayer1;
        this.gameStateMonitor = new GameStateMonitor();
    }


    public void startRoundTimer(int seconds, Runnable onTimeoutCallback) {
        if (currentTimer != null) {
            currentTimer.cancel();
        }

        currentTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {

                if (onTimeoutCallback != null) {
                    onTimeoutCallback.run();
                }

                markPlayerTimeout();
            }
        }.start();
    }


    public void checkIfOpponentAbandoned(Runnable onAbandonedCallback) {
        gameStateMonitor.detectAndHandleAbandonment(matchId, getOpponentUserId(), 
                com.google.firebase.auth.FirebaseAuth.getInstance().getUid())
                .addOnSuccessListener(wasAbandoned -> {
                    if (Boolean.TRUE.equals(wasAbandoned)) {
                        showToast("Protivnik je napustio igru!");
                        if (onAbandonedCallback != null) {
                            onAbandonedCallback.run();
                        }
                    }
                });
    }


    public boolean shouldSkipOpponentTimer() {

        return false;
    }


    private void markPlayerTimeout() {
        gameStateMonitor.handlePlayerTimeout(matchId, isPlayer1);
    }


    public void stopTimer() {
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
    }


    private String getOpponentUserId() {
        return null;  // Pre-implement kao parametar
    }

    private void showToast(String message) {
        if (parentFragment.getContext() != null) {
            Toast.makeText(parentFragment.getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    public void cleanup() {
        stopTimer();
    }
}

