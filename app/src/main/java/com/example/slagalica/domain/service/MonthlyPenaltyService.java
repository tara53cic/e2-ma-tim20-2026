package com.example.slagalica.domain.service;

import com.example.slagalica.data.NotificationRepository;
import com.example.slagalica.domain.models.NotificationType;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class MonthlyPenaltyService {

    private final FirebaseFirestore db;
    private final LeagueService leagueService;
    private final LeagueManager leagueManager;
    private final NotificationRepository notificationRepository;

    public MonthlyPenaltyService() {
        db = FirebaseFirestore.getInstance();
        leagueService = new LeagueService();
        leagueManager = new LeagueManager();
        notificationRepository = new NotificationRepository();
    }

    public void applyPenaltyIfNeeded(String uid, String lastResetMonth) {
        if (uid == null) return;

        String currentMonth = getCurrentMonth();
        if (currentMonth.equals(lastResetMonth)) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Long monthlyStarsLong = doc.getLong("monthlyStars");
                    Long starsLong = doc.getLong("stars");
                    Long leagueLong = doc.getLong("league");

                    int monthlyStars = monthlyStarsLong != null ? monthlyStarsLong.intValue() : 0;
                    int stars = starsLong != null ? starsLong.intValue() : 0;
                    int league = leagueLong != null ? leagueLong.intValue() : 0;

                    boolean placed = monthlyStars > 0;

                    if (!placed && stars > 0) {
                        int newStars = (int) (stars * 0.70);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("stars", newStars);
                        updates.put("monthlyStars", 0);
                        updates.put("lastResetMonth", currentMonth);

                        db.collection("users").document(uid).update(updates)
                                .addOnSuccessListener(v -> {
                                    leagueManager.updateLeagueIfNeeded(uid, newStars, league);

                                    int lost = stars - newStars;
                                    notificationRepository.createNotification(
                                            "Mesečna penalizacija",
                                            "Niste se plasirali na rang listi. Izgubili ste " +
                                                    lost + " zvezda (-30%). 📉",
                                            NotificationType.RANKING
                                    );
                                });
                    } else {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("monthlyStars", 0);
                        updates.put("lastResetMonth", currentMonth);
                        db.collection("users").document(uid).update(updates);
                    }
                });
    }

    private String getCurrentMonth() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return cal.get(java.util.Calendar.YEAR) + "-"
                + String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1);
    }
}