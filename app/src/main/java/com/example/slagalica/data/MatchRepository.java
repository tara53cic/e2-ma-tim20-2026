package com.example.slagalica.data;

import com.example.slagalica.domain.models.Match;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class MatchRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public MatchRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public Task<Void> createMatch(Match match) {
        if (match.getId() == null || match.getId().isEmpty()) {
            DocumentReference newMatchRef = db.collection("matches").document();
            match.setId(newMatchRef.getId());
            return newMatchRef.set(match);
        } else {
            return db.collection("matches").document(match.getId()).set(match);
        }
    }

    public DocumentReference getMatchReference(String matchId) {
        return db.collection("matches").document(matchId);
    }

    public Task<Void> updateScore(String matchId, boolean isPlayer1, int score) {
        String fieldScope = isPlayer1 ? "player1_score" : "player2_score";
        return db.collection("matches").document(matchId).update(fieldScope, score);
    }

    public Task<Void> updateMatchStatus(String matchId, String status) {
        return db.collection("matches").document(matchId).update("status", status);
    }

    public Task<QuerySnapshot> findAvailableMatch() {
        return db.collection("matches")
                .whereEqualTo("status", "WAITING")
                .limit(10)
                .get();
    }

    public Task<Void> deleteWaitingMatches(String userId) {
        return db.collection("matches")
                .whereEqualTo("player1_id", userId)
                .whereEqualTo("status", "WAITING")
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            doc.getReference().delete();
                        }
                    }
                    return Tasks.forResult(null);
                });
    }

    public Task<Void> joinMatch(String matchId, String player2Id) {
        return db.collection("matches").document(matchId)
                .update("player2_id", player2Id, "status", "IN_PROGRESS");
    }

    public String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    public Task<Void> recordPlayerAbandonment(String matchId, String abandonedByUserId) {
        return db.collection("matches").document(matchId)
                .update("abandonedBy", abandonedByUserId, "status", "FINISHED");
    }

    // Upisuje da je igrač napustio MatchFragment dok je partija bila u toku - trenutan,
    // pouzdan signal (za razliku od sporog online/inGame pollinga). Ne diramo "status" ovde
    // jer partija treba da NASTAVI za prisutnog igrača do kraja svih igara.
    public Task<Void> markPlayerLeft(String matchId, String userId) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("abandonedBy", userId);
        return db.collection("matches").document(matchId)
                .set(data, com.google.firebase.firestore.SetOptions.merge());
    }

    public Task<Void> recordPlayerTimeout(String matchId, boolean isPlayer1) {
        String field = isPlayer1 ? "player1_timedOut" : "player2_timedOut";
        return db.collection("matches").document(matchId).update(field, true);
    }

    public Task<DocumentSnapshot> getMatch(String matchId) {
        return db.collection("matches").document(matchId).get();
    }
}

