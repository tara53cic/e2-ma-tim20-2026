package com.example.slagalica.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
public class AssociationsRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<QuerySnapshot> fetchActivePuzzles() {
        return db.collection("association")
                .whereEqualTo("active", true)
                .get();
    }
}
