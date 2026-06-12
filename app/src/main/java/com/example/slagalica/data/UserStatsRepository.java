package com.example.slagalica.data;

import com.example.slagalica.domain.models.UserGameStats;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserStatsRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private DocumentReference statsDoc(String uid) {
        return db.collection("users").document(uid)
                .collection("stats").document("gameStats");
    }

    public Task<DocumentSnapshot> getStats(String uid) {
        return statsDoc(uid).get();
    }

    public void recordMatchResult(String uid, boolean win) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("totalMatches", FieldValue.increment(1));
        updates.put(win ? "wins" : "losses", FieldValue.increment(1));
        statsDoc(uid).set(updates, com.google.firebase.firestore.SetOptions.merge());
    }

    public void recordKzz(String uid, int correct, int incorrect, int points) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("kzzGames",          FieldValue.increment(1));
        updates.put("kzzTotalQuestions", FieldValue.increment(correct + incorrect));
        updates.put("kzzCorrect",        FieldValue.increment(correct));
        updates.put("kzzIncorrect",      FieldValue.increment(incorrect));
        updates.put("kzzTotalPoints",    FieldValue.increment(points));
        statsDoc(uid).set(updates, com.google.firebase.firestore.SetOptions.merge());
    }

    public void recordSpojnice(String uid, int connected, int totalPairs, int points) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("spojniceGames",      FieldValue.increment(1));
        updates.put("spojniceTotalPairs", FieldValue.increment(totalPairs));
        updates.put("spojniceConnected",  FieldValue.increment(connected));
        updates.put("spojniceTotalPoints",FieldValue.increment(points));
        statsDoc(uid).set(updates, com.google.firebase.firestore.SetOptions.merge());
    }

    public void recordAsocijacije(String uid, boolean solved, int points) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("asocijacijeGames",       FieldValue.increment(1));
        updates.put("asocijacijeTotal",        FieldValue.increment(1));
        if (solved) updates.put("asocijacijeSolved", FieldValue.increment(1));
        updates.put("asocijacijeTotalPoints",  FieldValue.increment(points));
        statsDoc(uid).set(updates, com.google.firebase.firestore.SetOptions.merge());
    }

    public void recordSkocko(String uid, int solvedAtAttempt, int points) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("skockoGames",       FieldValue.increment(1));
        updates.put("skockoTotalPoints", FieldValue.increment(points));
        if (solvedAtAttempt >= 0 && solvedAtAttempt <= 5) {
            updates.put("skockoSolvedAt" + solvedAtAttempt, FieldValue.increment(1));
        }
        statsDoc(uid).set(updates, com.google.firebase.firestore.SetOptions.merge());
    }

    public void recordKpk(String uid, int solvedAtStep, int points) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("kpkGames",       FieldValue.increment(1));
        updates.put("kpkTotalPoints", FieldValue.increment(points));
        if (solvedAtStep >= 0 && solvedAtStep <= 6) {
            updates.put("kpkSolvedAt" + solvedAtStep, FieldValue.increment(1));
        }
        statsDoc(uid).set(updates, com.google.firebase.firestore.SetOptions.merge());
    }

    public void recordMojBroj(String uid, boolean exact, int points) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("mojBrojGames",       FieldValue.increment(1));
        updates.put("mojBrojTotalPoints", FieldValue.increment(points));
        if (exact) updates.put("mojBrojExact", FieldValue.increment(1));
        statsDoc(uid).set(updates, com.google.firebase.firestore.SetOptions.merge());
    }
    public String getCurrentUid() {
        return auth.getUid();
    }
}