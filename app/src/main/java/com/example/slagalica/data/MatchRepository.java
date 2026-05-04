package com.example.slagalica.data;

import com.example.slagalica.domain.models.Match;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

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

    public String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }
}

