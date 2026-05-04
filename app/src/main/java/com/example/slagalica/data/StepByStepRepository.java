package com.example.slagalica.data;

import com.example.slagalica.domain.models.StepByStep;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class StepByStepRepository {

    private final FirebaseFirestore db;

    public StepByStepRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<QuerySnapshot> fetchTwoGames() {
        return db.collection("step_by_step")
                .limit(2)
                .get();
    }
}
