package com.example.slagalica.data;

import com.example.slagalica.domain.models.FriendRequest;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

public class FriendRequestRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FriendRequestRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public String getMyUid() {
        return auth.getUid();
    }

    public Task<DocumentReference> sendRequest(FriendRequest request) {
        return db.collection("friend_requests").add(request);
    }

    public Task<Void> updateStatus(String requestId, String status) {
        return db.collection("friend_requests").document(requestId)
                .update("status", status);
    }

    public Task<Void> setMatchId(String requestId, String matchId) {
        return db.collection("friend_requests").document(requestId)
                .update("matchId", matchId, "status", "ACCEPTED");
    }

    public ListenerRegistration listenIncoming(String myUid,
                                               EventListener<QuerySnapshot> listener) {
        return db.collection("friend_requests")
                .whereEqualTo("toUid", myUid)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener(listener);
    }

    public ListenerRegistration listenOutgoing(String requestId,
                                               EventListener<DocumentSnapshot> listener) {
        return db.collection("friend_requests").document(requestId)
                .addSnapshotListener(listener);
    }

    public Task<DocumentSnapshot> getRequest(String requestId) {
        return db.collection("friend_requests").document(requestId).get();
    }
}