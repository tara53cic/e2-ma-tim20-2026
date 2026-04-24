package com.example.slagalica.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public Task<Void> deductTokens(int amount) {
        String uid = auth.getUid();
        if (uid == null) return null;
        
        return db.collection("users").document(uid)
                .update("tokens", FieldValue.increment(-amount));
    }
}

