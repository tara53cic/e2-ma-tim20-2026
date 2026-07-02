package com.example.slagalica.domain.service;

import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.NotificationRepository;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.domain.models.NotificationType;
import com.google.android.gms.tasks.Task;


public class GameFlowManager {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public GameFlowManager() {
        this.matchRepository = new MatchRepository();
        this.userRepository = new UserRepository();
        this.notificationRepository = new NotificationRepository();
    }


    public Task<Void> handlePlayerAbandonment(String matchId, String abandonedByUserId, String opponentUserId) {
        Task<Void> abandonmentTask = matchRepository.recordPlayerAbandonment(matchId, abandonedByUserId);


        if (opponentUserId != null) {
            notificationRepository.createNotificationForUser(
                    opponentUserId,
                    "Igrač je napustio partiju",
                    "Protivnik je napustio partiju. Partija je završena a vi ste pobedili bez osvajanja zvezda.",
                    NotificationType.OTHER
            );
        }

        return abandonmentTask;
    }


    public Task<Void> handlePlayerTimeout(String matchId, boolean isPlayer1) {
        return matchRepository.recordPlayerTimeout(matchId, isPlayer1);
    }


    public Task<Void> checkAndHandleAbandonment(String matchId, String abandonedByUserId, String opponentUserId) {

        return matchRepository.getMatch(matchId).continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                String status = task.getResult().getString("status");
                String currentAbandoned = task.getResult().getString("abandonedBy");

                if ("IN_PROGRESS".equals(status) && currentAbandoned == null) {
                    return handlePlayerAbandonment(matchId, abandonedByUserId, opponentUserId);
                }
            }
            return com.google.android.gms.tasks.Tasks.forResult(null);
        });
    }


    public long getMinimumWaitTimeForAbsentPlayer() {
        return 0; // No wait, process immediately
    }
}

