package com.example.slagalica.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.slagalica.domain.models.ChatMessage;
import com.example.slagalica.domain.models.Challenge;
import java.util.HashMap;
import java.util.Map;

public class RegionRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public RegionRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public Task<QuerySnapshot> getPlayersByRegion(String region) {
        return db.collection("users")
                .whereEqualTo("region", region)
                .get();
    }

    public Task<QuerySnapshot> getActivePlayersByRegion(String region) {
        return db.collection("users")
                .whereEqualTo("region", region)
                .whereEqualTo("online", true)
                .get();
    }

    public Task<QuerySnapshot> getAllPlayers() {
        return db.collection("users").get();
    }

    public Task<QuerySnapshot> getAllRegionMedals() {
        return db.collection("region_medals").get();
    }

    public String getCurrentUid() {
        return auth.getUid() != null ? auth.getUid() : "";
    }

    // Chat
    public Task<DocumentReference> sendMessage(String region, ChatMessage message) {
        return db.collection("regions").document(region)
                .collection("messages").add(message);
    }

    public Query getChatMessages(String region) {
        return db.collection("regions").document(region)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    // Challenges
    public Task<DocumentReference> createChallenge(String region, Challenge challenge) {
        DocumentReference ref = db.collection("regions").document(region)
                .collection("challenges").document();
        challenge.setId(ref.getId());
        return ref.set(challenge).continueWith(task -> ref);
    }

    public Query getChallenges(String region) {
        return db.collection("regions").document(region)
                .collection("challenges")
                .whereIn("status", java.util.Arrays.asList("OPEN", "IN_PROGRESS"));
    }

    public Task<Void> joinChallenge(String region, String challengeId, String userId, String userName) {
        DocumentReference ref = db.collection("regions").document(region)
                .collection("challenges").document(challengeId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("playerIds", FieldValue.arrayUnion(userId));
        updates.put("playerNames." + userId, userName);
        
        return ref.update(updates);
    }

    public Task<Void> updateChallengeStatus(String region, String challengeId, String status) {
        return db.collection("regions").document(region)
                .collection("challenges").document(challengeId)
                .update("status", status);
    }

    public Task<Void> updateChallengeScore(String region, String challengeId, String userId, int score) {
        DocumentReference ref = db.collection("regions").document(region)
                .collection("challenges").document(challengeId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("playerScores." + userId, score);
        updates.put("playersFinished." + userId, true);
        
        return ref.update(updates);
    }

    public DocumentReference getChallengeReference(String region, String challengeId) {
        return db.collection("regions").document(region)
                .collection("challenges").document(challengeId);
    }
}