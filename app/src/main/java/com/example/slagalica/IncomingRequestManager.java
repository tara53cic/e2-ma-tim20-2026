package com.example.slagalica;

import android.app.Activity;
import android.app.AlertDialog;

import com.example.slagalica.data.FriendRequestRepository;
import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.domain.models.FriendRequest;
import com.example.slagalica.domain.models.Match;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import android.content.Intent;
import android.os.CountDownTimer;

public class IncomingRequestManager {

    private final Activity activity;
    private final FriendRequestRepository requestRepo;
    private final MatchRepository matchRepo;
    private ListenerRegistration listener;
    private CountDownTimer autoDeclineTimer;
    private AlertDialog currentDialog;

    public IncomingRequestManager(Activity activity) {
        this.activity = activity;
        this.requestRepo = new FriendRequestRepository();
        this.matchRepo = new MatchRepository();
    }

    public void start() {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        listener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("friend_requests")
                .whereEqualTo("toUid", myUid)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    if (snapshots.isEmpty()) {
                        activity.runOnUiThread(() -> dismissDialog());
                        stopTimer();
                        return;
                    }

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        FriendRequest req = doc.toObject(FriendRequest.class);
                        if (req == null) continue;
                        req.setId(doc.getId());

                        if (currentDialog == null || !currentDialog.isShowing()) {
                            activity.runOnUiThread(() -> showDialog(req));
                            startTimer(req.getId());
                        }
                        break;
                    }
                });
    }

    public void stop() {
        if (listener != null) listener.remove();
        stopTimer();
        dismissDialog();
    }

    private void showDialog(FriendRequest request) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        dismissDialog();

        currentDialog = new AlertDialog.Builder(activity)
                .setTitle("Poziv za partiju!")
                .setMessage(request.getFromUsername() + " te poziva na prijateljsku partiju!")
                .setPositiveButton("Prihvati", (d, w) -> acceptRequest(request))
                .setNegativeButton("Odbij", (d, w) -> declineRequest(request))
                .setCancelable(false)
                .create();
        currentDialog.show();
    }

    private void dismissDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
        currentDialog = null;
    }

    private void acceptRequest(FriendRequest request) {
        stopTimer();
        String myUid = FirebaseAuth.getInstance().getUid();
        Match match = new Match(null, request.getFromUid(), myUid, 0, 0, "IN_PROGRESS");

        matchRepo.createMatch(match).addOnSuccessListener(v -> {
            requestRepo.setMatchId(request.getId(), match.getId())
                    .addOnSuccessListener(v2 -> {
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.putExtra("NAVIGATE_TO", "MATCH");
                        intent.putExtra("MATCH_ID", match.getId());
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        activity.startActivity(intent);
                    });
        });
    }

    private void declineRequest(FriendRequest request) {
        stopTimer();
        requestRepo.updateStatus(request.getId(), "DECLINED");
    }

    private void startTimer(String requestId) {
        stopTimer();
        autoDeclineTimer = new CountDownTimer(10000, 1000) {
            @Override public void onTick(long ms) {}
            @Override public void onFinish() {
                requestRepo.updateStatus(requestId, "DECLINED");
                activity.runOnUiThread(() -> dismissDialog());
            }
        }.start();
    }

    private void stopTimer() {
        if (autoDeclineTimer != null) {
            autoDeclineTimer.cancel();
            autoDeclineTimer = null;
        }
    }
}