package com.example.slagalica.domain.service;

import com.example.slagalica.data.NotificationRepository;
import com.example.slagalica.domain.models.NotificationType;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LeagueManager {

    private final FirebaseFirestore db;
    private final LeagueService leagueService;
    private final NotificationRepository notificationRepository;

    public LeagueManager() {
        db = FirebaseFirestore.getInstance();
        leagueService = new LeagueService();
        notificationRepository = new NotificationRepository();
    }

    public void updateLeagueIfNeeded(String uid, int newStars, int currentLeague) {
        LeagueService.LeagueResult result = leagueService.calculateLeague(newStars, currentLeague);

        if (result.newLeague == currentLeague) return; // Nema promene, ne ažuriraj

        Map<String, Object> updates = new HashMap<>();
        updates.put("league", result.newLeague);

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(v -> {
                    // Pošalji notifikaciju igraču
                    String leagueName = LeagueService.getLeagueName(result.newLeague);
                    if (result.promoted) {
                        notificationRepository.createNotification(
                                "Napredovanje u ligu!",
                                "Čestitamo! Prešli ste u " + leagueName + " ligu! 🎉",
                                NotificationType.REWARDS
                        );
                    } else {
                        notificationRepository.createNotification(
                                "Pad u nižu ligu",
                                "Pali ste na " + leagueName + " ligu. Trudite se više! 💪",
                                NotificationType.OTHER
                        );
                    }
                });
    }
}