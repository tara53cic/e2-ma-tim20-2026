package com.example.slagalica.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FriendRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getCurrentUid() {
        return auth.getUid();
    }

    public Task<QuerySnapshot> searchByUsername(String username) {
        String end = username.substring(0, username.length() - 1)
                + (char)(username.charAt(username.length() - 1) + 1);
        return db.collection("users")
                .whereGreaterThanOrEqualTo("username", username)
                .whereLessThan("username", end)
                .limit(10)
                .get();
    }

    public Task<Void> addFriend(String friendUid) {
        String myUid = getCurrentUid();
        if (myUid == null) return null;

        Map<String, Object> data = new HashMap<>();
        data.put("since", System.currentTimeMillis());

        db.collection("users").document(myUid)
                .collection("friends").document(friendUid)
                .set(data);

        return db.collection("users").document(friendUid)
                .collection("friends").document(myUid)
                .set(data);
    }

    public Task<Void> removeFriend(String friendUid) {
        String myUid = getCurrentUid();
        if (myUid == null) return null;

        db.collection("users").document(friendUid)
                .collection("friends").document(myUid)
                .delete();

        return db.collection("users").document(myUid)
                .collection("friends").document(friendUid)
                .delete();
    }

    public Task<QuerySnapshot> getFriendIds() {
        String myUid = getCurrentUid();
        if (myUid == null) return null;
        return db.collection("users").document(myUid)
                .collection("friends")
                .get();
    }

    public Task<DocumentSnapshot> getUserById(String uid) {
        return db.collection("users").document(uid).get();
    }

    public Task<DocumentSnapshot> checkFriendship(String friendUid) {
        String myUid = getCurrentUid();
        if (myUid == null) return null;
        return db.collection("users").document(myUid)
                .collection("friends").document(friendUid)
                .get();
    }

    public String getMyUid() {
        return getCurrentUid();
    }
}