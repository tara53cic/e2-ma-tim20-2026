package com.example.slagalica.ui.match.matching;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.MatchingRepository;
import com.example.slagalica.domain.models.MatchingData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MatchingViewModel extends ViewModel {

    private final MatchingRepository repository;

    private final MutableLiveData<List<MatchingData>> rounds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);

    public MatchingViewModel() {
        repository = new MatchingRepository();
    }

    public void loadAndPublish(String matchId) {
        isLoading.setValue(true);
        repository.fetchAllRounds()
                .addOnSuccessListener(querySnapshot -> {
                    List<MatchingData> all = querySnapshot.toObjects(MatchingData.class);
                    Collections.shuffle(all);
                    // Uzmi 2 različita round-a
                    List<MatchingData> selected = new ArrayList<>(
                            all.size() >= 2 ? all.subList(0, 2) : all);
                    repository.publishRounds(matchId, selected)
                            .addOnSuccessListener(v -> {
                                rounds.setValue(selected);
                                isLoading.setValue(false);
                            })
                            .addOnFailureListener(e -> {
                                rounds.setValue(new ArrayList<>());
                                isLoading.setValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    rounds.setValue(new ArrayList<>());
                    isLoading.setValue(false);
                });
    }

    public void loadFromMatch(String matchId) {
        isLoading.setValue(true);
        repository.fetchPublishedRounds(matchId)
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        rounds.setValue(new ArrayList<>());
                        isLoading.setValue(false);
                        return;
                    }
                    List<MatchingData> loaded = new ArrayList<>();
                    for (int i = 0; i < 2; i++) {
                        Object raw = snapshot.get("round" + i);
                        if (raw instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            MatchingData md = parseMatchingData(
                                    (java.util.Map<String, Object>) raw);
                            if (md != null) loaded.add(md);
                        }
                    }
                    rounds.setValue(loaded);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    rounds.setValue(new ArrayList<>());
                    isLoading.setValue(false);
                });
    }

    @SuppressWarnings("unchecked")
    private MatchingData parseMatchingData(java.util.Map<String, Object> map) {
        MatchingData md = new MatchingData();
        md.setTitle((String) map.get("title"));
        md.setLeft_items((List<String>) map.get("left_items"));
        md.setRight_items((List<String>) map.get("right_items"));
        List<Object> rawPairs = (List<Object>) map.get("correct_pairs");
        if (rawPairs != null) {
            List<Long> pairs = new ArrayList<>();
            for (Object o : rawPairs) pairs.add(((Number) o).longValue());
            md.setCorrect_pairs(pairs);
        }
        return md;
    }

    public LiveData<List<MatchingData>> getRounds() { return rounds; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
}