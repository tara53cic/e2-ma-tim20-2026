package com.example.slagalica.ui.match.match_result;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.domain.service.GameResultService;
import com.example.slagalica.domain.service.LeagueManager;
import com.example.slagalica.domain.service.UserStatsService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MatchResultViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final GameResultService gameResultService;
    private final UserStatsService userStatsService;
    private final LeagueManager leagueManager;

    private final MutableLiveData<GameResultService.GameResult> gameResult = new MutableLiveData<>();
    private final MutableLiveData<UserStatsService.UserStatsResult> statsResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public MatchResultViewModel() {
        this.userRepository = new UserRepository();
        this.matchRepository = new MatchRepository();
        this.gameResultService = new GameResultService();
        this.userStatsService = new UserStatsService();
        this.leagueManager = new LeagueManager();
    }

    public LiveData<GameResultService.GameResult> getGameResult() { return gameResult; }
    public LiveData<UserStatsService.UserStatsResult> getStatsResult() { return statsResult; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void calculateAndSaveStats(boolean isPlayer1, int p1Score, int p2Score) {
        calculateAndSaveStats(isPlayer1, p1Score, p2Score, null, false, false);
    }

    public void calculateAndSaveStats(boolean isPlayer1, int p1Score, int p2Score,
                                      String matchId, boolean isAbandonment, boolean isTimeout) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        isLoading.setValue(true);

        int myScore = isPlayer1 ? p1Score : p2Score;
        int opponentScore = isPlayer1 ? p2Score : p1Score;


        if (matchId != null) {
            matchRepository.getMatch(matchId).addOnSuccessListener(matchDoc -> {
                if (matchDoc.exists()) {
                    boolean isFriendly = matchDoc.getBoolean("friendly") != null && matchDoc.getBoolean("friendly");
                    // Ako je meč zavrsen jer je protivnik napustio partiju, ovaj klijent
                    // je uvek onaj koji je ostao - tretiramo ga kao pobednika bez obzira na skor.
                    boolean wasAbandoned = isAbandonment || matchDoc.getString("abandonedBy") != null;
                    processStats(currentUser.getUid(), myScore, opponentScore, isFriendly, wasAbandoned, isTimeout);
                } else {
                    processStats(currentUser.getUid(), myScore, opponentScore, false, isAbandonment, isTimeout);
                }
            }).addOnFailureListener(e -> {
                processStats(currentUser.getUid(), myScore, opponentScore, false, isAbandonment, isTimeout);
            });
        } else {
            processStats(currentUser.getUid(), myScore, opponentScore, false, isAbandonment, isTimeout);
        }
    }

    private void processStats(String uid, int myScore, int opponentScore,
                             boolean isFriendly, boolean isAbandonment, boolean isTimeout) {
        userRepository.getUser(uid).addOnSuccessListener(doc -> {
            if (!doc.exists()) { isLoading.postValue(false); return; }

            Long currentStarsLong = doc.getLong("stars");
            Long currentTokensLong = doc.getLong("tokens");
            Long currentLeagueLong = doc.getLong("league");
            Long monthlyStarsLong = doc.getLong("monthlyStars");

            int currentStars = currentStarsLong != null ? currentStarsLong.intValue() : 0;
            int currentTokens = currentTokensLong != null ? currentTokensLong.intValue() : 0;
            int currentLeague = currentLeagueLong != null ? currentLeagueLong.intValue() : 0;
            int currentMonthly = monthlyStarsLong != null ? monthlyStarsLong.intValue() : 0;

            GameResultService.GameResult result = gameResultService.calculateGameResult(
                    myScore, opponentScore, currentStars, currentTokens,
                    isFriendly, isAbandonment, isTimeout
            );

            gameResult.postValue(result);


            if (isFriendly) {
                isLoading.postValue(false);
                return;
            }


            final int starsBeforeConversion = Math.max(0, currentStars + result.totalStarsChange);

            int monthlyStarsNew = currentMonthly;
            if (result.totalStarsChange > 0) {
                monthlyStarsNew += result.totalStarsChange;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("stars", result.newStars);
            updates.put("tokens", result.newTokens);
            updates.put("monthlyStars", monthlyStarsNew);

            FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .update(updates)
                    .addOnCompleteListener(task -> {
                        leagueManager.updateLeagueIfNeeded(uid, starsBeforeConversion, currentLeague);


                        UserStatsService.UserStatsResult statsRes = new UserStatsService.UserStatsResult(
                                result.isWinner, result.totalStarsChange, result.newStars,
                                result.newTokens, result.tokensGained
                        );
                        statsResult.postValue(statsRes);
                        isLoading.postValue(false);
                    });

        }).addOnFailureListener(e -> isLoading.postValue(false));
    }
}