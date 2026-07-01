package com.example.slagalica.domain.service;

import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.NotificationRepository;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.domain.models.NotificationType;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;


public class GameStateMonitor {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public static final long ABANDONMENT_TIMEOUT_MS = 30000;
    public static final long ROUND_TIMEOUT_MS = 5000;

    public GameStateMonitor() {
        this.matchRepository = new MatchRepository();
        this.userRepository = new UserRepository();
        this.notificationRepository = new NotificationRepository();
    }


    public Task<Boolean> isPlayerActive(String userId) {
        return userRepository.getUser(userId)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (!doc.exists()) return false;

                        Boolean online = doc.getBoolean("online");
                        Boolean inGame = doc.getBoolean("inGame");


                        return Boolean.TRUE.equals(online) && Boolean.TRUE.equals(inGame);
                    }
                    return false;
                });
    }


    public Task<Void> detectAndHandleAbandonment(String matchId, String expectedPlayerId, String otherPlayerId) {
        return isPlayerActive(expectedPlayerId)
                .continueWithTask(task -> {
                    Boolean isActive = task.getResult();

                    if (Boolean.FALSE.equals(isActive)) {
                        // Igrač je napustio igru
                        return handleAbandonmentInternal(matchId, expectedPlayerId, otherPlayerId);
                    }
                    return com.google.android.gms.tasks.Tasks.forResult(null);
                });
    }


    public void startHeartbeat(String matchId, String playerId, String opponentId,
                               HeartbeatCallback callback) {

        new java.util.Timer().scheduleAtFixedRate(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        isPlayerActive(playerId)
                                .addOnSuccessListener(isActive -> {
                                    if (!isActive && callback != null) {
                                        callback.onPlayerInactive();
                                        this.cancel();
                                    }
                                });
                    }
                },
                3000,
                3000
        );
    }


    public Task<Void> handlePlayerTimeout(String matchId, boolean isPlayer1) {
        return matchRepository.recordPlayerTimeout(matchId, isPlayer1)
                .continueWithTask(task -> {

                    return com.google.android.gms.tasks.Tasks.forResult(null);
                });
    }


    private Task<Void> handleAbandonmentInternal(String matchId, String abandonedByUid, String opponentUid) {
        return matchRepository.recordPlayerAbandonment(matchId, abandonedByUid)
                .continueWithTask(task -> {

                    notificationRepository.createNotificationForUser(
                            opponentUid,
                            "Igrač je napustio partiju",
                            "Protivnik je napustio partiju! Pobedili ste!",
                            NotificationType.OTHER
                    );
                    return com.google.android.gms.tasks.Tasks.forResult(null);
                });
    }


    public interface HeartbeatCallback {
        void onPlayerInactive();
    }


    public long getMinWaitForAbsence() {
        return 0;
    }
}


