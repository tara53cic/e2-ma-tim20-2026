package com.example.slagalica.domain.service;

import com.example.slagalica.data.NotificationRepository;
import com.example.slagalica.domain.models.NotificationType;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DailyTokenService {

    private final FirebaseFirestore db;
    private final LeagueService leagueService;
    private final NotificationRepository notificationRepository;

    public DailyTokenService() {
        db = FirebaseFirestore.getInstance();
        leagueService = new LeagueService();
        notificationRepository = new NotificationRepository();
    }

    public void checkAndGiveDailyTokens(String uid) {
        if (uid == null) return;

        String today = getToday();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String lastBonus = doc.getString("lastDailyBonus");
                    if (today.equals(lastBonus)) return; // Već dobio danas

                    Long leagueLong = doc.getLong("league");
                    int league = leagueLong != null ? leagueLong.intValue() : 0;

                    int tokensToAdd = leagueService.getDailyTokenBonus(league); // 5 + liga

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("tokens", FieldValue.increment(tokensToAdd));
                    updates.put("lastDailyBonus", today);

                    db.collection("users").document(uid).update(updates)
                            .addOnSuccessListener(v -> {
                                notificationRepository.createNotification(
                                        "Dnevni bonus!",
                                        "Dobili ste " + tokensToAdd + " tokena za danas! 🎁",
                                        NotificationType.REWARDS
                                );
                            });
                });
    }

    private String getToday() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return cal.get(java.util.Calendar.YEAR) + "-"
                + String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
                + "-" + String.format("%02d", cal.get(java.util.Calendar.DAY_OF_MONTH));
    }
}