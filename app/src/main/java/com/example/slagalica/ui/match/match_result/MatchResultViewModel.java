package com.example.slagalica.ui.match.match_result;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.UserRepository;
import com.example.slagalica.domain.service.LeagueManager;
import com.example.slagalica.domain.service.UserStatsService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MatchResultViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final UserStatsService userStatsService;
    private final LeagueManager leagueManager;

    private final MutableLiveData<UserStatsService.UserStatsResult> statsResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public MatchResultViewModel() {
        this.userRepository = new UserRepository();
        this.userStatsService = new UserStatsService();
        this.leagueManager = new LeagueManager();
    }

    public LiveData<UserStatsService.UserStatsResult> getStatsResult() { return statsResult; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void calculateAndSaveStats(boolean isPlayer1, int p1Score, int p2Score) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        isLoading.setValue(true);

        int myScore       = isPlayer1 ? p1Score : p2Score;
        int opponentScore = isPlayer1 ? p2Score : p1Score;

        userRepository.getUser(currentUser.getUid()).addOnSuccessListener(doc -> {
            if (!doc.exists()) { isLoading.postValue(false); return; }

            Long currentStarsLong  = doc.getLong("stars");
            Long currentTokensLong = doc.getLong("tokens");
            Long currentLeagueLong = doc.getLong("league");
            Long monthlyStarsLong  = doc.getLong("monthlyStars");

            int currentStars   = currentStarsLong  != null ? currentStarsLong.intValue()  : 0;
            int currentTokens  = currentTokensLong != null ? currentTokensLong.intValue() : 0;
            int currentLeague  = currentLeagueLong != null ? currentLeagueLong.intValue() : 0;
            int currentMonthly = monthlyStarsLong  != null ? monthlyStarsLong.intValue()  : 0;

            UserStatsService.UserStatsResult result =
                    userStatsService.calculateNewStats(myScore, opponentScore, currentStars, currentTokens);

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
                    .collection("users").document(currentUser.getUid())
                    .update(updates)
                    .addOnCompleteListener(task -> {
                        leagueManager.updateLeagueIfNeeded(
                                currentUser.getUid(), starsBeforeConversion, currentLeague);
                        statsResult.postValue(result);
                        isLoading.postValue(false);
                    });

        }).addOnFailureListener(e -> isLoading.postValue(false));
    }
}