package com.example.slagalica.ui.match.match_result;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.UserRepository;
import com.example.slagalica.domain.service.UserStatsService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MatchResultViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final UserStatsService userStatsService;

    private final MutableLiveData<UserStatsService.UserStatsResult> statsResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public MatchResultViewModel() {
        this.userRepository = new UserRepository();
        this.userStatsService = new UserStatsService();
    }

    public LiveData<UserStatsService.UserStatsResult> getStatsResult() {
        return statsResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void calculateAndSaveStats(boolean isPlayer1, int p1Score, int p2Score) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        isLoading.setValue(true);

        int myScore = isPlayer1 ? p1Score : p2Score;
        int opponentScore = isPlayer1 ? p2Score : p1Score;

        userRepository.getUser(currentUser.getUid()).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long currentStarsLong = documentSnapshot.getLong("stars");
                Long currentTokensLong = documentSnapshot.getLong("tokens");

                int currentStars = currentStarsLong != null ? currentStarsLong.intValue() : 0;
                int currentTokens = currentTokensLong != null ? currentTokensLong.intValue() : 0;

                UserStatsService.UserStatsResult result = userStatsService.calculateNewStats(myScore, opponentScore, currentStars, currentTokens);

                userRepository.updateUserStats(currentUser.getUid(), result.newStars, result.newTokens).addOnCompleteListener(task -> {
                    statsResult.postValue(result);
                    isLoading.postValue(false);
                });
            } else {
                isLoading.postValue(false);
            }
        }).addOnFailureListener(e -> isLoading.postValue(false));
    }
}

