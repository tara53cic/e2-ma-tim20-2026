package com.example.slagalica.data;

import com.example.slagalica.domain.models.MatchingData;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchingRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<QuerySnapshot> fetchAllRounds() {
        return db.collection("matching").get();
    }

    public Task<Void> publishRounds(String matchId, List<MatchingData> rounds) {
        Map<String, Object> data = new HashMap<>();
        for (int i = 0; i < rounds.size(); i++) {
            data.put("round" + i, matchingDataToMap(rounds.get(i)));
        }
        return db.collection("matches").document(matchId)
                .collection("games").document("matching_data")
                .set(data);
    }

    public Task<DocumentSnapshot> fetchPublishedRounds(String matchId) {
        return db.collection("matches").document(matchId)
                .collection("games").document("matching_data")
                .get();
    }

    private Map<String, Object> matchingDataToMap(MatchingData md) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", md.getTitle());
        map.put("left_items", md.getLeft());
        map.put("right_items", md.getRight());
        map.put("correct_pairs", md.getCorrectMap());
        return map;
    }
}