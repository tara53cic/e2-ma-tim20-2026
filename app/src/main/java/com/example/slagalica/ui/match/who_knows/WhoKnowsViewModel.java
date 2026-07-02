package com.example.slagalica.ui.match.who_knows;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.WhoKnowsRepository;
import com.example.slagalica.domain.models.WhoKnowsQuestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WhoKnowsViewModel extends ViewModel {

    private final WhoKnowsRepository repository;

    private final MutableLiveData<List<WhoKnowsQuestion>> questions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);

    public WhoKnowsViewModel() {
        repository = new WhoKnowsRepository();
    }

    public void loadAndPublish(String matchId) {
        isLoading.setValue(true);
        repository.fetchAllQuestions()
                .addOnSuccessListener(querySnapshot -> {
                    List<WhoKnowsQuestion> all = querySnapshot.toObjects(WhoKnowsQuestion.class);
                    Collections.shuffle(all);
                    List<WhoKnowsQuestion> selected = new ArrayList<>(
                            all.size() >= 5 ? all.subList(0, 5) : all);
                    if (matchId == null) {
                        // Izazov (solo partija) nema match dokument u koji bi se objavilo.
                        questions.setValue(selected);
                        isLoading.setValue(false);
                        return;
                    }
                    repository.publishQuestions(matchId, selected)
                            .addOnSuccessListener(v -> {
                                questions.setValue(selected);
                                isLoading.setValue(false);
                            })
                            .addOnFailureListener(e -> {
                                questions.setValue(new ArrayList<>());
                                isLoading.setValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    questions.setValue(new ArrayList<>());
                    isLoading.setValue(false);
                });
    }

    public void loadFromMatch(String matchId) {
        isLoading.setValue(true);
        repository.fetchPublishedQuestions(matchId)
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        questions.setValue(new ArrayList<>());
                        isLoading.setValue(false);
                        return;
                    }
                    List<WhoKnowsQuestion> loaded = new ArrayList<>();
                    for (int i = 0; i < 5; i++) {
                        Object raw = snapshot.get("q" + i);
                        if (raw instanceof Map) {
                            @SuppressWarnings("unchecked")
                            WhoKnowsQuestion q = parseQuestion((Map<String, Object>) raw);
                            if (q != null) loaded.add(q);
                        }
                    }
                    questions.setValue(loaded);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    questions.setValue(new ArrayList<>());
                    isLoading.setValue(false);
                });
    }

    @SuppressWarnings("unchecked")
    private WhoKnowsQuestion parseQuestion(Map<String, Object> map) {
        WhoKnowsQuestion q = new WhoKnowsQuestion();
        q.setQuestion((String) map.get("question"));
        q.setAnswers((List<String>) map.get("answers"));
        Object ci = map.get("correct_index");
        if (ci instanceof Number) q.setCorrect_index(((Number) ci).intValue());
        return q;
    }

    public LiveData<List<WhoKnowsQuestion>> getQuestions() { return questions; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
}