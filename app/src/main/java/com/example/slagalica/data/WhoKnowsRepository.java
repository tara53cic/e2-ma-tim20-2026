package com.example.slagalica.data;

import com.example.slagalica.domain.models.WhoKnowsQuestion;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhoKnowsRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<QuerySnapshot> fetchAllQuestions() {
        return db.collection("who_knows").get();
    }

    public Task<Void> publishQuestions(String matchId, List<WhoKnowsQuestion> questions) {
        Map<String, Object> data = new HashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            data.put("q" + i, questionToMap(questions.get(i)));
        }
        return db.collection("matches").document(matchId)
                .collection("games").document("kzz_data")
                .set(data);
    }

    public Task<DocumentSnapshot> fetchPublishedQuestions(String matchId) {
        return db.collection("matches").document(matchId)
                .collection("games").document("kzz_data")
                .get();
    }

    private Map<String, Object> questionToMap(WhoKnowsQuestion q) {
        Map<String, Object> map = new HashMap<>();
        map.put("question", q.getQuestion());
        map.put("answers", q.getAnswers());
        map.put("correct_index", q.getCorrectIndex());
        return map;
    }
}