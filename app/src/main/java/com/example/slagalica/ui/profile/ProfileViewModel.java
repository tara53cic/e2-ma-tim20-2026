package com.example.slagalica.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileViewModel extends ViewModel {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<String> region = new MutableLiveData<>();
    private final MutableLiveData<Integer> tokens = new MutableLiveData<>();
    private final MutableLiveData<Integer> stars = new MutableLiveData<>();
    private final MutableLiveData<Integer> league = new MutableLiveData<>();
    private final MutableLiveData<Integer> avatarColorIndex = new MutableLiveData<>(0);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProfileViewModel() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        loadUserData();
    }

    public LiveData<String> getUsername() { return username; }
    public LiveData<String> getEmail() { return email; }
    public LiveData<String> getRegion() { return region; }
    public LiveData<Integer> getTokens() { return tokens; }
    public LiveData<Integer> getStars() { return stars; }
    public LiveData<Integer> getLeague() { return league; }
    public LiveData<Integer> getAvatarColorIndex() { return avatarColorIndex; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    private void loadUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        errorMessage.setValue("Greška pri učitavanju podataka.");
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) return;

                    username.setValue(snapshot.getString("username"));
                    email.setValue(snapshot.getString("email"));
                    region.setValue(snapshot.getString("region"));

                    Long tokensVal = snapshot.getLong("tokens");
                    Long starsVal = snapshot.getLong("stars");
                    Long leagueVal = snapshot.getLong("league");
                    Long avatarVal = snapshot.getLong("avatarColorIndex");

                    if (tokensVal != null) tokens.setValue(tokensVal.intValue());
                    if (starsVal != null) stars.setValue(starsVal.intValue());
                    if (leagueVal != null) league.setValue(leagueVal.intValue());
                    if (avatarVal != null) avatarColorIndex.setValue(avatarVal.intValue());
                });
    }
}