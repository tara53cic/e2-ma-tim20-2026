package com.example.slagalica.ui.game;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.os.CountDownTimer;

public class SharedGameViewModel extends ViewModel {
    private final MutableLiveData<Integer> timeRemaining = new MutableLiveData<>(60);
    private final MutableLiveData<Integer> player1Score = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> player2Score = new MutableLiveData<>(0);
    private final MutableLiveData<String> currentFragment = new MutableLiveData<>("MOJ_BROJ_R1");

    private CountDownTimer timer;
    private Runnable timerFinishAction;

    public LiveData<Integer> getTimeRemaining() { return timeRemaining; }
    public LiveData<Integer> getPlayer1Score() { return player1Score; }
    public LiveData<Integer> getPlayer2Score() { return player2Score; }
    public LiveData<String> getCurrentFragment() { return currentFragment; }

    public void startRoundTimer(int seconds, Runnable onFinish) {
        if (timer != null) timer.cancel();
        timeRemaining.setValue(seconds);
        this.timerFinishAction = onFinish;

        timer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining.postValue((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                timeRemaining.postValue(0);
                if (timerFinishAction != null) {
                    timerFinishAction.run();
                }
            }
        }.start();
    }

    public void stopTimer() {
        if (timer != null) timer.cancel();
    }

    public void addPlayer1Points(int points) {
        player1Score.setValue(player1Score.getValue() + points);
    }

    public void addPlayer2Points(int points) {
        player2Score.setValue(player2Score.getValue() + points);
    }

    public void advanceGamePhase() {
        String current = currentFragment.getValue();
        if ("MOJ_BROJ_R1".equals(current)) {
            currentFragment.setValue("MOJ_BROJ_R2");
        } else if ("MOJ_BROJ_R2".equals(current)) {
            currentFragment.setValue("KORAK_PO_KORAK_R1");
            startRoundTimer(70, this::advanceGamePhase);
        } else if ("KORAK_PO_KORAK_R1".equals(current)) {
            currentFragment.setValue("KORAK_PO_KORAK_R2");
            startRoundTimer(70, this::advanceGamePhase);
        } else {
            currentFragment.setValue("FINISHED");
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (timer != null) timer.cancel();
    }
}
