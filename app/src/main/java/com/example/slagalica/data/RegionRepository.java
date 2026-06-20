package com.example.slagalica.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class RegionRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public RegionRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public Task<QuerySnapshot> getPlayersByRegion(String region) {
        return db.collection("users")
                .whereEqualTo("region", region)
                .get();
    }

    public Task<QuerySnapshot> getActivePlayersByRegion(String region) {
        return db.collection("users")
                .whereEqualTo("region", region)
                .whereEqualTo("online", true)
                .get();
    }

    public Task<QuerySnapshot> getAllPlayers() {
        return db.collection("users").get();
    }

    public Task<QuerySnapshot> getAllRegionMedals() {
        return db.collection("region_medals").get();
    }

    public String getCurrentUid() {
        return auth.getUid() != null ? auth.getUid() : "";
    }
}