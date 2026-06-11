package com.example.slagalica.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class NumberGameRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DocumentReference gameDoc(String matchId, String gameKey) {
        return db.collection("matches").document(matchId)
                 .collection("games").document(gameKey);
    }

    public Task<Void> initRound(String matchId, String gameKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("targetNumber", "---");
        for (int i = 0; i < 6; i++) data.put("num" + i, "");
        data.put("lockPhase", 0);
        return gameDoc(matchId, gameKey).set(data);
    }

    public Task<Void> lockTarget(String matchId, String gameKey, String value) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("targetNumber", value);
        updates.put("lockPhase", 1);
        return gameDoc(matchId, gameKey).update(updates);
    }

    public Task<Void> lockNumber(String matchId, String gameKey, int slot, String value) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("num" + slot, value);
        updates.put("lockPhase", slot + 2);
        return gameDoc(matchId, gameKey).update(updates);
    }

    public Task<Void> submitResult(String matchId, String gameKey, boolean isPlayer1, long result) {
        Map<String, Object> updates = new HashMap<>();
        String prefix = isPlayer1 ? "p1" : "p2";
        updates.put(prefix + "Result", result);
        updates.put(prefix + "Submitted", true);
        return gameDoc(matchId, gameKey).update(updates);
    }

    public ListenerRegistration listen(String matchId, String gameKey,
                                       EventListener<DocumentSnapshot> listener) {
        return gameDoc(matchId, gameKey).addSnapshotListener(listener);
    }
}
