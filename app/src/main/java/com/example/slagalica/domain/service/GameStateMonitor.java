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
                    // Ako ne uspemo da dođemo do podataka (mreža npr), 
                    // optimistično pretpostavljamo da je igrač i dalje tu.
                    return true;
                });
    }


    public Task<Boolean> detectAndHandleAbandonment(String matchId, String expectedPlayerId, String otherPlayerId) {
        return isPlayerActive(expectedPlayerId)
                .continueWithTask(task -> {
                    Boolean isActive = task.getResult();

                    if (Boolean.FALSE.equals(isActive)) {
                        // Igrač je definitivno napustio igru
                        return handleAbandonmentInternal(matchId, expectedPlayerId, otherPlayerId)
                                .continueWith(t -> true);
                    }
                    return com.google.android.gms.tasks.Tasks.forResult(false);
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

    private static final int ABANDONMENT_INITIAL_DELAY_MS = 5000;
    private static final int ABANDONMENT_POLL_INTERVAL_MS = 6000;
    // Povećavamo broj potrebnih promašaja kako bismo bili manje osetljivi
    // na kratke prekide u mreži ili lag Firebase-a.
    private static final int ABANDONMENT_REQUIRED_MISSES = 5;

    /**
     * Periodično proverava da li je protivnik aktivan i poziva {@code onAbandoned}
     * tek nakon {@link #ABANDONMENT_REQUIRED_MISSES} uzastopnih neuspešnih provera.
     * Vraća {@link java.util.Timer} koji pozivalac mora otkazati (timer.cancel())
     * kad se fragment/igra uništi ili runda normalno završi.
     */
    public java.util.Timer startAbandonmentWatch(java.util.function.Supplier<String> opponentIdSupplier,
                                                  Runnable onAbandoned) {
        java.util.Timer timer = new java.util.Timer();
        final int[] missStreak = {0};
        timer.scheduleAtFixedRate(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        String opponentId = opponentIdSupplier.get();
                        if (opponentId == null) return;

                        isPlayerActive(opponentId).addOnSuccessListener(isActive -> {
                            if (isActive) {
                                missStreak[0] = 0;
                                return;
                            }
                            missStreak[0]++;
                            if (missStreak[0] >= ABANDONMENT_REQUIRED_MISSES) {
                                this.cancel();
                                onAbandoned.run();
                            }
                        });
                    }
                },
                ABANDONMENT_INITIAL_DELAY_MS,
                ABANDONMENT_POLL_INTERVAL_MS
        );
        return timer;
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


