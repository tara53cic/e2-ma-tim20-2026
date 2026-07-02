package com.example.slagalica.domain.service;

import com.example.slagalica.data.NotificationRepository;
import com.example.slagalica.domain.models.NotificationType;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class TokenService {

    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;

    public TokenService() {
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
    }


    public void giveDailyTokens(String uid) {
        if (uid == null) return;

        String today = getToday();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String lastBonus = doc.getString("lastDailyBonus");
                    if (today.equals(lastBonus)) return; // Already received today

                    Long leagueLong = doc.getLong("league");
                    int league = leagueLong != null ? leagueLong.intValue() : 0;

                    int tokensToAdd = 5 + (league / 2); // Base 5 tokens + bonus by league

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("tokens", FieldValue.increment(tokensToAdd));
                    updates.put("lastDailyBonus", today);

                    db.collection("users").document(uid).update(updates)
                            .addOnSuccessListener(v -> {
                                notificationRepository.createNotification(
                                        "Dnevni bonus tokena!",
                                        "Dobili ste " + tokensToAdd + " tokena! 🎁",
                                        NotificationType.REWARDS
                                );
                            });
                });
    }


    public boolean hasEnoughTokens(int currentTokens) {
        return currentTokens >= 1;
    }


    public int getTokensPerGame() {
        return 1;
    }


    public int getStarsPerToken() {
        return 50;
    }


    public int calculateTokensFromStarConversion(int prevStars, int newStars) {
        int tokensBefore = prevStars / getStarsPerToken();
        int tokensNow = newStars / getStarsPerToken();
        return Math.max(0, tokensNow - tokensBefore);
    }


    private String getToday() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return cal.get(java.util.Calendar.YEAR) + "-"
                + String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
                + "-" + String.format("%02d", cal.get(java.util.Calendar.DAY_OF_MONTH));
    }
}

