package com.example.slagalica.ui.match.step_by_step;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.StepByStepRepository;
import com.example.slagalica.domain.models.StepByStep;
import com.example.slagalica.domain.service.StepByStepScoringService;

import java.util.ArrayList;
import java.util.List;

public class StepByStepViewModel extends ViewModel {

    private final StepByStepRepository repository;
    private final StepByStepScoringService scoringService;

    private final MutableLiveData<List<StepByStep>> games = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);

    public StepByStepViewModel() {
        repository = new StepByStepRepository();
        scoringService = new StepByStepScoringService();
        loadGames();
    }

    private void loadGames() {
        isLoading.setValue(true);
        repository.fetchTwoGames().addOnSuccessListener(queryDocumentSnapshots -> {
            List<StepByStep> fetched = queryDocumentSnapshots.toObjects(StepByStep.class);
            games.setValue(fetched);
            isLoading.setValue(false);
        }).addOnFailureListener(e -> {
            isLoading.setValue(false);
        });
    }

    public LiveData<List<StepByStep>> getGames() {
        return games;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public int calculatePoints(int revealedStep, boolean isOpponentPhase) {
        return scoringService.calculatePoints(revealedStep, isOpponentPhase);
    }
}
