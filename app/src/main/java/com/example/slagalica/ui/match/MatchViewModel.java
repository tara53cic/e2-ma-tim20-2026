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
    private ListenerRegistration challengeResultListener;
    private boolean challengeRewardsProcessed = false;

    private final MutableLiveData<ChallengeOutcome> challengeOutcome = new MutableLiveData<>();

    public static class ChallengeOutcome {
        public final int placement;
        public final int totalPlayers;
        public final int myScore;
        public final int starsChange;
        public final int tokensChange;
        public final boolean isWinner;

        public ChallengeOutcome(int placement, int totalPlayers, int myScore,
                                 int starsChange, int tokensChange, boolean isWinner) {
            this.placement = placement;
            this.totalPlayers = totalPlayers;
            this.myScore = myScore;
            this.starsChange = starsChange;
            this.tokensChange = tokensChange;
            this.isWinner = isWinner;
        }
    }

    public LiveData<ChallengeOutcome> getChallengeOutcome() { return challengeOutcome; }

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
                int myScore = player1Score.getValue() != null ? player1Score.getValue() : 0;
                regionRepo.updateChallengeScore(regionId, challengeId, currentUserId, myScore)
                        .addOnSuccessListener(v -> listenForChallengeOutcome());
            }
        } else {
            currentFragment.setValue("FINISHED");
        }
    }

    // Svaki učesnik nezavisno prati izazov (nema centralnog "sudije"), pa se nagrade
    // moraju obračunati preko listenera na svakom uređaju čim svi igrači završe -
    // jednokratni get() bi nagradio samo pobednika ako je on slučajno poslednji koji završi.
    private void listenForChallengeOutcome() {
        if (challengeResultListener != null) return;
        challengeResultListener = regionRepo.getChallengeReference(regionId, challengeId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    Challenge challenge = snapshot.toObject(Challenge.class);
                    if (challenge == null) return;

                    Map<String, Boolean> finished = challenge.getPlayersFinished();
                    if (finished == null || finished.size() < challenge.getPlayerIds().size()) return;

                    List<Map.Entry<String, Integer>> sorted = new ArrayList<>(challenge.getPlayerScores().entrySet());
                    sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
                    if (sorted.isEmpty()) return;

                    int numPlayers = challenge.getPlayerIds().size();
                    int winnerStars = (int) (0.75 * numPlayers * challenge.getBidStars());
                    int winnerTokens = (int) (0.75 * numPlayers * challenge.getBidTokens());
                    String winnerId = sorted.get(0).getKey();
                    String secondId = sorted.size() > 1 ? sorted.get(1).getKey() : null;

                    int placement = numPlayers;
                    for (int i = 0; i < sorted.size(); i++) {
                        if (sorted.get(i).getKey().equals(currentUserId)) {
                            placement = i + 1;
                            break;
                        }
                    }

                    boolean isWinner = currentUserId.equals(winnerId);
                    int starsChange = 0;
                    int tokensChange = 0;
                    if (isWinner) {
                        starsChange = winnerStars;
                        tokensChange = winnerTokens;
                    } else if (secondId != null && currentUserId.equals(secondId)) {
                        starsChange = challenge.getBidStars();
                        tokensChange = challenge.getBidTokens();
                    }

                    Integer myScore = challenge.getPlayerScores().get(currentUserId);
                    challengeOutcome.postValue(new ChallengeOutcome(
                            placement, numPlayers, myScore != null ? myScore : 0,
                            starsChange, tokensChange, isWinner));

                    if (!challengeRewardsProcessed && !"FINISHED".equals(challenge.getStatus())) {
                        challengeRewardsProcessed = true;
                        if (starsChange > 0) userRepository.addStars(starsChange);
                        if (tokensChange > 0) userRepository.addTokens(tokensChange);
                        regionRepo.updateChallengeStatus(regionId, challengeId, "FINISHED");
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (timer != null) timer.cancel();
        if (matchListener != null) matchListener.remove();
        if (challengeResultListener != null) challengeResultListener.remove();
    }

    public void shouldDeductToken(boolean isFriendly) {
        if (matchId != null && !isTokenDeducted && !isFriendly && !isChallenge) {
            userRepository.deductTokens(1);
            isTokenDeducted = true;
        }
    }
}
