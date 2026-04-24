package com.example.slagalica.data;

import com.example.slagalica.domain.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public Task<AuthResult> registerUser(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    public Task<Void> saveUserToFirestore(String uid, User user) {
        return db.collection("users").document(uid).set(user);
    }

    public Task<Void> sendEmailVerification() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            return user.sendEmailVerification();
        }
        return null;
    }

    public Task<AuthResult> loginUser(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    public Task<Void> sendPasswordReset(String email) {
        return auth.sendPasswordResetEmail(email);
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void logout() {
        auth.signOut();
    }
}

