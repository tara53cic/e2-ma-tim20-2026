package com.example.slagalica.ui.match;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.os.CountDownTimer;

import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.domain.models.Match;
import com.google.firebase.firestore.ListenerRegistration;

public class MatchViewModel extends ViewModel {
    private final MutableLiveData<Integer> timeRemaining = new MutableLiveData<>(60);
    private final MutableLiveData<Integer> player1Score = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> player2Score = new MutableLiveData<>(0);
    private final MutableLiveData<String> currentFragment = new MutableLiveData<>("MOJ_BROJ_R1");

    private CountDownTimer timer;
    private Runnable timerFinishAction;

    private MatchRepository matchRepository = new MatchRepository();
    private String matchId;
    private String currentUserId;
    private ListenerRegistration matchListener;
    private boolean isPlayer1 = true; // default to true

    public String getMatchId() {
        return matchId;
    }

    public boolean getIsPlayer1() {
        return isPlayer1;
    }

    public LiveData<Integer> getTimeRemaining() { return timeRemaining; }
    public LiveData<Integer> getPlayer1Score() { return player1Score; }
    public LiveData<Integer> getPlayer2Score() { return player2Score; }
    public LiveData<String> getCurrentFragment() { return currentFragment; }

    private final MutableLiveData<String> player1Name = new MutableLiveData<>("Gost");
    private final MutableLiveData<String> player2Name = new MutableLiveData<>("Gost");

    public LiveData<String> getPlayer1Name() { return player1Name; }
    public LiveData<String> getPlayer2Name() { return player2Name; }

    public void initMatch(String matchId) {
        this.matchId = matchId;
        this.currentUserId = matchRepository.getCurrentUserId();

        if (matchId != null) {
            matchListener = matchRepository.getMatchReference(matchId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null || snapshot == null || !snapshot.exists()) return;
                        Match match = snapshot.toObject(Match.class);
                        if (match != null) {
                            // Which player are we?
                            if (currentUserId != null) {
                                isPlayer1 = currentUserId.equals(match.getPlayer1_id());
                            }

                            if (player1Score.getValue() == null || player1Score.getValue() != match.getPlayer1_score()) {
                                player1Score.setValue(match.getPlayer1_score());
                            }
                            if (player2Score.getValue() == null || player2Score.getValue() != match.getPlayer2_score()) {
                                player2Score.setValue(match.getPlayer2_score());
                            }

                            // Fetch Usernames
                            UserRepository userRepository = new UserRepository();
                            if (match.getPlayer1_id() != null) {
                                userRepository.getUser(match.getPlayer1_id()).addOnSuccessListener(doc -> {
                                    if (doc.exists() && doc.getString("username") != null && !doc.getString("username").isEmpty()) {
                                        player1Name.setValue(doc.getString("username"));
                                    }
                                });
                            }
                            if (match.getPlayer2_id() != null) {
                                userRepository.getUser(match.getPlayer2_id()).addOnSuccessListener(doc -> {
                                    if (doc.exists() && doc.getString("username") != null && !doc.getString("username").isEmpty()) {
                                        player2Name.setValue(doc.getString("username"));
                                    }
                                });
                            }
                        }
                    });
        }
    }

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

    public void addCurrentPlayerPoints(int points) {
        if (isPlayer1) {
            int newScore = (player1Score.getValue() != null ? player1Score.getValue() : 0) + points;
            player1Score.setValue(newScore);
            if (matchId != null) {
                matchRepository.updateScore(matchId, true, newScore);
            }
        } else {
            int newScore = (player2Score.getValue() != null ? player2Score.getValue() : 0) + points;
            player2Score.setValue(newScore);
            if (matchId != null) {
                matchRepository.updateScore(matchId, false, newScore);
            }
        }
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
        if (matchListener != null) matchListener.remove();
    }
}
