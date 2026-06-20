package com.example.slagalica.ui.friends;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.FriendRepository;
import com.example.slagalica.domain.models.Friend;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendsViewModel extends ViewModel {

    private final FriendRepository repository;

    private final MutableLiveData<List<Friend>> friends = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Friend>> searchResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public FriendsViewModel() {
        repository = new FriendRepository();
    }

    public LiveData<List<Friend>> getFriends() { return friends; }
    public LiveData<List<Friend>> getSearchResults() { return searchResults; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void loadFriends() {
        isLoading.setValue(true);
        repository.getFriendIds().addOnSuccessListener(querySnapshot -> {
            List<String> ids = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                ids.add(doc.getId());
            }
            if (ids.isEmpty()) {
                friends.setValue(new ArrayList<>());
                isLoading.setValue(false);
                return;
            }
            fetchFriendDetails(ids);
        }).addOnFailureListener(e -> {
            errorMessage.setValue("Greška pri učitavanju prijatelja.");
            isLoading.setValue(false);
        });
    }

    private void fetchFriendDetails(List<String> ids) {
        List<Friend> result = new ArrayList<>();
        final int[] counter = {0};

        for (String uid : ids) {
            repository.getUserById(uid).addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Friend f = documentToFriend(uid, doc);
                    result.add(f);
                }
                counter[0]++;
                if (counter[0] == ids.size()) {
                    friends.setValue(result);
                    isLoading.setValue(false);
                }
            }).addOnFailureListener(e -> {
                counter[0]++;
                if (counter[0] == ids.size()) {
                    friends.setValue(result);
                    isLoading.setValue(false);
                }
            });
        }
    }

    public void searchUsers(String username) {
        if (username == null || username.trim().isEmpty()) {
            searchResults.setValue(new ArrayList<>());
            return;
        }
        repository.searchByUsername(username.trim()).addOnSuccessListener(querySnapshot -> {
            List<Friend> results = new ArrayList<>();
            String myUid = repository.getMyUid();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (doc.getId().equals(myUid)) continue;
                Friend f = documentToFriend(doc.getId(), doc);
                results.add(f);
            }
            searchResults.setValue(results);
        }).addOnFailureListener(e -> {
            errorMessage.setValue("Greška pri pretrazi.");
        });
    }

    public void addFriend(String friendUid) {
        repository.checkFriendship(friendUid).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                errorMessage.setValue("Već ste prijatelji.");
                return;
            }
            repository.addFriend(friendUid).addOnSuccessListener(v -> {
                successMessage.setValue("Prijatelj dodat!");
                loadFriends();
            }).addOnFailureListener(e -> {
                errorMessage.setValue("Greška pri dodavanju prijatelja.");
            });
        }).addOnFailureListener(e -> {
            errorMessage.setValue("Greška.");
        });
    }

    public void removeFriend(String friendUid) {
        repository.removeFriend(friendUid).addOnSuccessListener(v -> {
            successMessage.setValue("Prijatelj uklonjen.");
            loadFriends();
        }).addOnFailureListener(e -> {
            errorMessage.setValue("Greška pri uklanjanju prijatelja.");
        });
    }

    private Friend documentToFriend(String uid, DocumentSnapshot doc) {
        Friend f = new Friend();
        f.setUid(uid);
        f.setUsername(doc.getString("username"));
        f.setEmail(doc.getString("email"));
        Long starsVal = doc.getLong("stars");
        Long leagueVal = doc.getLong("league");
        Long avatarVal = doc.getLong("avatarColorIndex");
        f.setStars(starsVal != null ? starsVal.intValue() : 0);
        f.setLeague(leagueVal != null ? leagueVal.intValue() : 0);
        f.setAvatarColorIndex(avatarVal != null ? avatarVal.intValue() : 0);
        return f;
    }
}