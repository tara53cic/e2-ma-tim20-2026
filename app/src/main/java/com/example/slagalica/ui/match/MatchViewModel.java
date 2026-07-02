package com.example.slagalica.ui.match;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.os.CountDownTimer;

import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.data.RegionRepository;
import com.example.slagalica.domain.models.Match;
import com.example.slagalica.domain.models.Challenge;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatchViewModel extends ViewModel {
    private final MutableLiveData<Integer> timeRemaining = new MutableLiveData<>(60);
    private final MutableLiveData<Integer> player1Score = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> player2Score = new MutableLiveData<>(0);
    private final MutableLiveData<String> currentFragment = new MutableLiveData<>("WAITING");

    private final MutableLiveData<String> player1Name = new MutableLiveData<>("Gost");
    private final MutableLiveData<String> player2Name = new MutableLiveData<>("Gost");

    private final MutableLiveData<Integer> player1AvatarIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> player2AvatarIndex = new MutableLiveData<>(0);

    private final MutableLiveData<Boolean> isOpponentAbandoned = new MutableLiveData<>(false);

    private CountDownTimer timer;
    private Runnable timerFinishAction;

    private final MatchRepository matchRepository = new MatchRepository();
    private final UserRepository userRepository = new UserRepository();
    private final RegionRepository regionRepo = new RegionRepository();
    
    private String matchId;
    private String currentUserId;
    private ListenerRegistration matchListener;
    private boolean isPlayer1 = false;
    private boolean isTokenDeducted = false;
    
    private boolean isChallenge = false;
    private String challengeId;
    private String regionId;

    public String getMatchId() { return matchId; }
    public boolean getIsPlayer1() { return isPlayer1; }
    public boolean isChallenge() { return isChallenge; }
    public String getChallengeId() { return challengeId; }

    public LiveData<Integer> getTimeRemaining() { return timeRemaining; }
    public LiveData<Integer> getPlayer1Score() { return player1Score; }
    public LiveData<Integer> getPlayer2Score() { return player2Score; }
    public LiveData<String> getCurrentFragment() { return currentFragment; }
    public LiveData<String> getPlayer1Name() { return player1Name; }
    public LiveData<String> getPlayer2Name() { return player2Name; }
    public LiveData<Integer> getPlayer1AvatarIndex() { return player1AvatarIndex; }
    public LiveData<Integer> getPlayer2AvatarIndex() { return player2AvatarIndex; }
    public LiveData<Boolean> getIsOpponentAbandoned() { return isOpponentAbandoned; }
    public void setOpponentAbandoned(boolean abandoned) { isOpponentAbandoned.setValue(abandoned); }

    public void initMatch(String matchId) {
        this.matchId = matchId;
        this.isChallenge = false;
        this.currentUserId = matchRepository.getCurrentUserId();
        
        player1Score.setValue(0);
        player2Score.setValue(0);
        currentFragment.setValue("WAITING");

        if (matchId != null) {
            matchListener = matchRepository.getMatchReference(matchId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null || snapshot == null || !snapshot.exists()) return;
                        Match match = snapshot.toObject(Match.class);
                        if (match != null) {
                            if (match.getStatus() != null && !"WAITING".equals(match.getStatus())) {
                                shouldDeductToken(match.isFriendly());
                            }
                            if (currentUserId == null) currentUserId = matchRepository.getCurrentUserId();
                            if (currentUserId != null) isPlayer1 = currentUserId.equals(match.getPlayer1_id());

                            if (match.getPlayer2_id() != null && "WAITING".equals(currentFragment.getValue())) {
                                currentFragment.postValue("MOJ_BROJ_R1");
                            }

                            if (player1Score.getValue() == null || player1Score.getValue() != match.getPlayer1_score()) {
                                player1Score.setValue(match.getPlayer1_score());
                            }
                            if (player2Score.getValue() == null || player2Score.getValue() != match.getPlayer2_score()) {
                                player2Score.setValue(match.getPlayer2_score());
                            }

                            if (match.getPlayer1_id() != null) {
                                userRepository.getUser(match.getPlayer1_id()).addOnSuccessListener(doc -> {
                                    if (!doc.exists()) return;
                                    player1Name.setValue(doc.getString("username"));
                                    Long avatarIdx = doc.getLong("avatarColorIndex");
                                    player1AvatarIndex.setValue(avatarIdx != null ? avatarIdx.intValue() : 0);
                                });
                            }

                            if (match.getPlayer2_id() != null) {
                                userRepository.getUser(match.getPlayer2_id()).addOnSuccessListener(doc -> {
                                    if (!doc.exists()) return;
                                    player2Name.setValue(doc.getString("username"));
                                    Long avatarIdx = doc.getLong("avatarColorIndex");
                                    player2AvatarIndex.setValue(avatarIdx != null ? avatarIdx.intValue() : 0);
                                });
                            }
                        }
                    });
        }
    }

    public void initChallenge(String challengeId, String regionId) {
        this.challengeId = challengeId;
        this.regionId = regionId;
        this.isChallenge = true;
        this.isPlayer1 = true;
        this.currentUserId = matchRepository.getCurrentUserId();
        
        player1Score.setValue(0);
        player2Score.setValue(0);
        
        userRepository.getUser(currentUserId).addOnSuccessListener(doc -> {
            player1Name.setValue(doc.getString("username"));
            Long avatarIdx = doc.getLong("avatarColorIndex");
            player1AvatarIndex.setValue(avatarIdx != null ? avatarIdx.intValue() : 0);
        });
        
        player2Name.setValue("Izazov");
        player2AvatarIndex.setValue(0);
        currentFragment.postValue("MOJ_BROJ_R1");
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
                if (timerFinishAction != null) timerFinishAction.run();
            }
        }.start();
    }

    public void stopTimer() {
        if (timer != null) timer.cancel();
    }

    public void addCurrentPlayerPoints(int points) {
        if (isChallenge) {
            int current = player1Score.getValue() != null ? player1Score.getValue() : 0;
            player1Score.setValue(current + points);
            return;
        }
        if (isPlayer1) {
            int newScore = (player1Score.getValue() != null ? player1Score.getValue() : 0) + points;
            player1Score.setValue(newScore);
            if (matchId != null) matchRepository.updateScore(matchId, true, newScore);
        } else {
            int newScore = (player2Score.getValue() != null ? player2Score.getValue() : 0) + points;
            player2Score.setValue(newScore);
            if (matchId != null) matchRepository.updateScore(matchId, false, newScore);
        }
    }

    public void advanceGamePhase() {
        String current = currentFragment.getValue();
        if ("MOJ_BROJ_R1".equals(current)) {
            if (isChallenge) {
                currentFragment.setValue("KORAK_PO_KORAK_R1");
                startRoundTimer(70, this::advanceGamePhase);
            } else {
                currentFragment.setValue("MOJ_BROJ_R2");
            }
        } else if ("MOJ_BROJ_R2".equals(current)) {
            currentFragment.setValue("KORAK_PO_KORAK_R1");
            startRoundTimer(70, this::advanceGamePhase);
        } else if ("KORAK_PO_KORAK_R1".equals(current)) {
            if (isChallenge) {
                currentFragment.setValue("SPOJNICE_R1");
            } else {
                currentFragment.setValue("KORAK_PO_KORAK_R2");
                startRoundTimer(70, this::advanceGamePhase);
            }
        } else if ("KORAK_PO_KORAK_R2".equals(current)) {
            currentFragment.setValue("SPOJNICE_R1");
        } else if ("SPOJNICE_R1".equals(current)) {
            if (isChallenge) {
                currentFragment.setValue("SKOCKO_R1");
            } else {
                currentFragment.setValue("SPOJNICE_R2");
            }
        } else if ("SPOJNICE_R2".equals(current)) {
            currentFragment.setValue("SKOCKO_R1");
        } else if ("SKOCKO_R1".equals(current)) {
            if (isChallenge) {
                currentFragment.setValue("ASOCIJACIJE_R1");
            } else {
                currentFragment.setValue("SKOCKO_R2");
            }
        } else if ("SKOCKO_R2".equals(current)) {
            currentFragment.setValue("ASOCIJACIJE_R1");
        } else if ("ASOCIJACIJE_R1".equals(current)) {
            if (isChallenge) {
                currentFragment.setValue("KZZ");
            } else {
                currentFragment.setValue("ASOCIJACIJE_R2");
            }
        } else if ("ASOCIJACIJE_R2".equals(current)) {
            currentFragment.setValue("KZZ");
        } else if ("KZZ".equals(current)) {
            currentFragment.setValue("FINISHED");
            if (isChallenge) {
                if (player1Score.getValue() != null) {
                    regionRepo.updateChallengeScore(regionId, challengeId, currentUserId, player1Score.getValue())
                        .addOnSuccessListener(v -> checkAndDistributeRewards());
                }
            }
        } else {
            currentFragment.setValue("FINISHED");
        }
    }

    private void checkAndDistributeRewards() {
        regionRepo.getChallengeReference(regionId, challengeId).get().addOnSuccessListener(snapshot -> {
            Challenge challenge = snapshot.toObject(Challenge.class);
            if (challenge == null || !"IN_PROGRESS".equals(challenge.getStatus())) return;
            if (challenge.getPlayersFinished().size() == challenge.getPlayerIds().size()) {
                distributeChallengeRewards(challenge);
            }
        });
    }

    private void distributeChallengeRewards(Challenge challenge) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(challenge.getPlayerScores().entrySet());
        sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        if (sorted.isEmpty()) return;

        int numPlayers = challenge.getPlayerIds().size();
        int winnerStars = (int) (0.75 * numPlayers * challenge.getBidStars());
        int winnerTokens = (int) (0.75 * numPlayers * challenge.getBidTokens());
        String winnerId = sorted.get(0).getKey();
        String secondId = sorted.size() > 1 ? sorted.get(1).getKey() : null;

        regionRepo.updateChallengeStatus(regionId, challengeId, "FINISHED").addOnSuccessListener(v -> {
            if (currentUserId.equals(winnerId)) {
                userRepository.addStars(winnerStars).addOnSuccessListener(v2 -> userRepository.addTokens(winnerTokens));
            } else if (secondId != null && currentUserId.equals(secondId)) {
                userRepository.addStars(challenge.getBidStars()).addOnSuccessListener(v2 -> userRepository.addTokens(challenge.getBidTokens()));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (timer != null) timer.cancel();
        if (matchListener != null) matchListener.remove();
    }

    public void shouldDeductToken(boolean isFriendly) {
        if (matchId != null && !isTokenDeducted && !isFriendly) {
            userRepository.deductTokens(1);
            isTokenDeducted = true;
        }
    }
}
