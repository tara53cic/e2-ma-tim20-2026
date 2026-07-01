package com.example.slagalica.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.UserStatsRepository;
import com.example.slagalica.domain.models.UserGameStats;
import com.google.firebase.firestore.DocumentSnapshot;

public class StatisticsViewModel extends ViewModel {

    private final UserStatsRepository statsRepo = new UserStatsRepository();

    private final MutableLiveData<UserGameStats> stats = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<UserGameStats> getStats()     { return stats; }
    public LiveData<Boolean>       getIsLoading() { return isLoading; }
    public LiveData<String>        getError()     { return error; }

    public StatisticsViewModel() {
        loadStats();
    }

    public void loadStats() {
        String uid = statsRepo.getCurrentUid();
        if (uid == null) {
            error.setValue("Korisnik nije prijavljen.");
            isLoading.setValue(false);
            return;
        }

        isLoading.setValue(true);
        statsRepo.getStats(uid).addOnSuccessListener(snapshot -> {
            if (snapshot != null && snapshot.exists()) {
                UserGameStats s = parseStats(snapshot);
                stats.setValue(s);
            } else {
                stats.setValue(new UserGameStats());
            }
            isLoading.setValue(false);
        }).addOnFailureListener(e -> {
            error.setValue("Greška pri učitavanju statistike.");
            isLoading.setValue(false);
        });
    }

    private UserGameStats parseStats(DocumentSnapshot doc) {
        UserGameStats s = new UserGameStats();

        s.setTotalMatches(getLong(doc, "totalMatches"));
        s.setWins(getLong(doc, "wins"));
        s.setLosses(getLong(doc, "losses"));

        // Ko zna zna
        s.setKzzGames(getLong(doc, "kzzGames"));
        s.setKzzTotalQuestions(getLong(doc, "kzzTotalQuestions"));
        s.setKzzCorrect(getLong(doc, "kzzCorrect"));
        s.setKzzIncorrect(getLong(doc, "kzzIncorrect"));
        s.setKzzTotalPoints(getLong(doc, "kzzTotalPoints"));

        // Spojnice
        s.setSpojniceGames(getLong(doc, "spojniceGames"));
        s.setSpojniceTotalPairs(getLong(doc, "spojniceTotalPairs"));
        s.setSpojniceConnected(getLong(doc, "spojniceConnected"));
        s.setSpojniceTotalPoints(getLong(doc, "spojniceTotalPoints"));

        // Asocijacije
        s.setAsocijacijeGames(getLong(doc, "asocijacijeGames"));
        s.setAsocijacijeTotal(getLong(doc, "asocijacijeTotal"));
        s.setAsocijacijeSolved(getLong(doc, "asocijacijeSolved"));
        s.setAsocijacijeTotalPoints(getLong(doc, "asocijacijeTotalPoints"));

        // Skočko
        s.setSkockoGames(getLong(doc, "skockoGames"));
        s.setSkockoTotalPoints(getLong(doc, "skockoTotalPoints"));
        s.setSkockoSolvedAt0(getLong(doc, "skockoSolvedAt0"));
        s.setSkockoSolvedAt1(getLong(doc, "skockoSolvedAt1"));
        s.setSkockoSolvedAt2(getLong(doc, "skockoSolvedAt2"));
        s.setSkockoSolvedAt3(getLong(doc, "skockoSolvedAt3"));
        s.setSkockoSolvedAt4(getLong(doc, "skockoSolvedAt4"));
        s.setSkockoSolvedAt5(getLong(doc, "skockoSolvedAt5"));

        // Korak po korak
        s.setKpkGames(getLong(doc, "kpkGames"));
        s.setKpkTotalPoints(getLong(doc, "kpkTotalPoints"));
        s.setKpkSolvedAt0(getLong(doc, "kpkSolvedAt0"));
        s.setKpkSolvedAt1(getLong(doc, "kpkSolvedAt1"));
        s.setKpkSolvedAt2(getLong(doc, "kpkSolvedAt2"));
        s.setKpkSolvedAt3(getLong(doc, "kpkSolvedAt3"));
        s.setKpkSolvedAt4(getLong(doc, "kpkSolvedAt4"));
        s.setKpkSolvedAt5(getLong(doc, "kpkSolvedAt5"));
        s.setKpkSolvedAt6(getLong(doc, "kpkSolvedAt6"));

        // Moj broj
        s.setMojBrojGames(getLong(doc, "mojBrojGames"));
        s.setMojBrojExact(getLong(doc, "mojBrojExact"));
        s.setMojBrojTotalPoints(getLong(doc, "mojBrojTotalPoints"));

        return s;
    }

    private int getLong(DocumentSnapshot doc, String field) {
        Long val = doc.getLong(field);
        return val != null ? val.intValue() : 0;
    }
}