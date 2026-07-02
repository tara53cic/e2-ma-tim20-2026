package com.example.slagalica.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Map;


public class GameStateRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DocumentReference gameDoc(String matchId, String gameKey) {
        return db.collection("matches").document(matchId)
                 .collection("games").document(gameKey);
    }

    public Task<Void> set(String matchId, String gameKey, Map<String, Object> data) {
        return gameDoc(matchId, gameKey).set(data);
    }

    public Task<Void> update(String matchId, String gameKey, Map<String, Object> fields) {
        // set+merge umesto update(): kad protivnik napusti partiju pre nego što je
        // ovaj dokument uopšte kreiran, prisutni igrač i dalje mora moći da upiše
        // stanje (update() bi tiho failovao sa NOT_FOUND na nepostojećem dokumentu).
        return gameDoc(matchId, gameKey).set(fields, com.google.firebase.firestore.SetOptions.merge());
    }

    public ListenerRegistration listen(String matchId, String gameKey,
                                       EventListener<DocumentSnapshot> listener) {
        return gameDoc(matchId, gameKey).addSnapshotListener(listener);
    }
}
