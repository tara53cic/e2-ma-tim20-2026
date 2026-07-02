package com.example.slagalica.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public Task<Void> deductTokens(int amount) {
        String uid = auth.getUid();
        if (uid == null) return null;
        return db.collection("users").document(uid)
                .update("tokens", FieldValue.increment(-amount));
    }

    public Task<Void> deductStars(int amount) {
        String uid = auth.getUid();
        if (uid == null) return null;
        return db.collection("users").document(uid)
                .update("stars", FieldValue.increment(-amount));
    }

    public Task<Void> addTokens(int amount) {
        String uid = auth.getUid();
        if (uid == null) return null;
        return db.collection("users").document(uid)
                .update("tokens", FieldValue.increment(amount));
    }

    public Task<Void> addStars(int amount) {
        String uid = auth.getUid();
        if (uid == null) return null;
        return db.collection("users").document(uid)
                .update("stars", FieldValue.increment(amount));
    }

    public Task<Void> addMonthlyStars(String uid, int amount) {
        if (uid == null) return null;
        return db.collection("users").document(uid)
                .update("monthlyStars", FieldValue.increment(amount));
    }

    public Task<DocumentSnapshot> getUser(String uid) {
        return db.collection("users").document(uid).get();
    }

    public Task<Void> updateUserStats(String uid, int newStars, int newTokens) {
        return db.collection("users").document(uid)
                .update("stars", newStars, "tokens", newTokens);
    }

    public Task<Void> updateUserStatsWithMonthly(String uid, int newStars,
                                                 int newTokens, int starsChange) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("stars", newStars);
        updates.put("tokens", newTokens);
        // monthlyStars se povećava samo ako je promjena pozitivna
        if (starsChange > 0) {
            updates.put("monthlyStars", FieldValue.increment(starsChange));
        }
        return db.collection("users").document(uid).update(updates);
    }

    public void checkAndResetMonthlyStars(String uid) {
        if (uid == null) return;

        String currentMonth = getCurrentMonth();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String lastReset = doc.getString("lastResetMonth");

                    if (lastReset == null || !lastReset.equals(currentMonth)) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("monthlyStars", 0);
                        updates.put("lastResetMonth", currentMonth);
                        db.collection("users").document(uid).update(updates);
                    }
                });
    }

    public void setInGame(boolean inGame) {
        String uid = auth.getUid();
        if (uid == null) return;
        db.collection("users").document(uid).update("inGame", inGame);
    }

    public Task<Void> initializeNewUser(String uid, String email, String username, String region) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("username", username);
        userData.put("region", region);
        userData.put("tokens", 5);
        userData.put("stars", 0);
        userData.put("league", 0);
        userData.put("monthlyStars", 0);
        userData.put("lastResetMonth", getCurrentMonth());
        userData.put("lastDailyBonus", "");
        userData.put("createdAt", System.currentTimeMillis());

        return db.collection("users").document(uid).set(userData);
    }

    private String getCurrentMonth() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return cal.get(java.util.Calendar.YEAR) + "-"
                + String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1);
    }
}