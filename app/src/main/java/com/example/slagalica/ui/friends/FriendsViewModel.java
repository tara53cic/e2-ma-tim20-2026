package com.example.slagalica.ui.friends;

import android.os.CountDownTimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.FriendRepository;
import com.example.slagalica.data.FriendRequestRepository;
import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.NotificationRepository;
import com.example.slagalica.domain.models.Friend;
import com.example.slagalica.domain.models.FriendRequest;
import com.example.slagalica.domain.models.Match;
import com.example.slagalica.domain.models.NotificationType;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendsViewModel extends ViewModel {

    private final FriendRepository friendRepo;
    private final FriendRequestRepository requestRepo;
    private final MatchRepository matchRepo;
    private final NotificationRepository notificationRepo;

    private final MutableLiveData<List<Friend>> friends = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Friend>> searchResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> pendingRequestId = new MutableLiveData<>();
    private final MutableLiveData<FriendRequest> incomingRequest = new MutableLiveData<>();
    private final MutableLiveData<String> navigateToMatch = new MutableLiveData<>();


    private ListenerRegistration outgoingListener;
    private CountDownTimer autoDeclineTimer;

    public FriendsViewModel() {
        friendRepo = new FriendRepository();
        requestRepo = new FriendRequestRepository();
        matchRepo = new MatchRepository();
        notificationRepo = new NotificationRepository();
    }

    public LiveData<List<Friend>> getFriends() { return friends; }
    public LiveData<List<Friend>> getSearchResults() { return searchResults; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getPendingRequestId() { return pendingRequestId; }
    public LiveData<FriendRequest> getIncomingRequest() { return incomingRequest; }
    public LiveData<String> getNavigateToMatch() { return navigateToMatch; }

    public void loadFriends() {
        isLoading.setValue(true);
        friendRepo.getFriendIds().addOnSuccessListener(querySnapshot -> {
            List<String> ids = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) ids.add(doc.getId());
            if (ids.isEmpty()) { friends.setValue(new ArrayList<>()); isLoading.setValue(false); return; }
            fetchFriendDetails(ids);
        }).addOnFailureListener(e -> { errorMessage.setValue("Greška."); isLoading.setValue(false); });
    }

    private void fetchFriendDetails(List<String> ids) {
        List<Friend> result = new ArrayList<>();
        final int[] counter = {0};
        for (String uid : ids) {
            friendRepo.getUserById(uid).addOnSuccessListener(doc -> {
                if (doc.exists()) result.add(documentToFriend(uid, doc));
                counter[0]++;
                if (counter[0] == ids.size()) {
                    calculateMonthlyRanks(result);
                    isLoading.setValue(false);
                }
            }).addOnFailureListener(e -> {
                counter[0]++;
                if (counter[0] == ids.size()) {
                    calculateMonthlyRanks(result);
                    isLoading.setValue(false);
                }
            });
        }
    }

    public void searchUsers(String username) {
        if (username == null || username.trim().isEmpty()) {
            searchResults.setValue(new ArrayList<>()); return;
        }
        friendRepo.searchByUsername(username.trim()).addOnSuccessListener(querySnapshot -> {
            List<Friend> results = new ArrayList<>();
            String myUid = friendRepo.getMyUid();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (doc.getId().equals(myUid)) continue;
                results.add(documentToFriend(doc.getId(), doc));
            }
            searchResults.setValue(results);
        }).addOnFailureListener(e -> errorMessage.setValue("Greška pri pretrazi."));
    }

    public void addFriend(String friendUid) {
        friendRepo.checkFriendship(friendUid).addOnSuccessListener(doc -> {
            if (doc.exists()) { errorMessage.setValue("Već ste prijatelji."); return; }
            friendRepo.addFriend(friendUid).addOnSuccessListener(v -> {
                successMessage.setValue("Prijatelj dodat!");
                loadFriends();
            }).addOnFailureListener(e -> errorMessage.setValue("Greška pri dodavanju."));
        });
    }

    public void addFriendByUid(String friendUid) {
        String myUid = friendRepo.getMyUid();
        if (myUid == null || friendUid.equals(myUid)) {
            errorMessage.setValue("Ne možeš dodati sebe.");
            return;
        }
        friendRepo.checkFriendship(friendUid).addOnSuccessListener(doc -> {
            if (doc.exists()) { errorMessage.setValue("Već ste prijatelji."); return; }
            friendRepo.getUserById(friendUid).addOnSuccessListener(userDoc -> {
                if (!userDoc.exists()) {
                    errorMessage.setValue("Korisnik nije pronađen.");
                    return;
                }
                friendRepo.addFriend(friendUid).addOnSuccessListener(v -> {
                    successMessage.setValue("Prijatelj dodat: " + userDoc.getString("username"));
                    loadFriends();
                }).addOnFailureListener(e -> errorMessage.setValue("Greška pri dodavanju."));
            });
        });
    }

    public void removeFriend(String friendUid) {
        friendRepo.removeFriend(friendUid).addOnSuccessListener(v -> {
            successMessage.setValue("Prijatelj uklonjen.");
            loadFriends();
        }).addOnFailureListener(e -> errorMessage.setValue("Greška pri uklanjanju."));
    }

    public void sendGameRequest(Friend friend) {
        String myUid = friendRepo.getMyUid();
        if (myUid == null) return;

        friendRepo.getUserById(myUid).addOnSuccessListener(doc -> {
            String myUsername = doc.getString("username");
            if (myUsername == null) myUsername = "Igrač";

            FriendRequest request = new FriendRequest(
                    null, myUid, myUsername,
                    friend.getUid(), "PENDING",
                    System.currentTimeMillis()
            );

            final String finalUsername = myUsername;
            requestRepo.sendRequest(request).addOnSuccessListener(ref -> {
                String requestId = ref.getId();
                pendingRequestId.setValue(requestId);

                notificationRepo.createNotificationForUser(
                        friend.getUid(),
                        "Zahtev za partiju",
                        finalUsername + " te poziva na prijateljsku partiju!",
                        NotificationType.OTHER
                );

                listenForResponse(requestId);

                startAutoDeclineTimer(requestId);

            }).addOnFailureListener(e -> errorMessage.setValue("Greška pri slanju zahteva."));
        });
    }

    public void cancelRequest() {
        String requestId = pendingRequestId.getValue();
        if (requestId == null) return;

        stopAutoDeclineTimer();
        if (outgoingListener != null) outgoingListener.remove();

        requestRepo.updateStatus(requestId, "CANCELLED")
                .addOnSuccessListener(v -> {
                    pendingRequestId.setValue(null);
                    successMessage.setValue("Zahtev otkazan.");
                });
    }

    public void acceptRequest(FriendRequest request) {
        stopAutoDeclineTimer();

        // Kreiraj match
        Match match = new Match(null, request.getFromUid(),
                friendRepo.getMyUid(), 0, 0, "IN_PROGRESS");
        match.setFriendly(true);

        matchRepo.createMatch(match).addOnSuccessListener(v -> {
            requestRepo.setMatchId(request.getId(), match.getId())
                    .addOnSuccessListener(v2 -> {
                        incomingRequest.setValue(null);
                        navigateToMatch.setValue(match.getId());
                    });
        });
    }

    public void declineRequest(FriendRequest request) {
        stopAutoDeclineTimer();
        requestRepo.updateStatus(request.getId(), "DECLINED")
                .addOnSuccessListener(v -> incomingRequest.setValue(null));
    }

    private void listenForResponse(String requestId) {
        if (outgoingListener != null) outgoingListener.remove();
        outgoingListener = requestRepo.listenOutgoing(requestId, (snap, e) -> {
            if (e != null || snap == null || !snap.exists()) return;
            String status = snap.getString("status");
            if ("ACCEPTED".equals(status)) {
                stopAutoDeclineTimer();
                String matchId = snap.getString("matchId");
                if (matchId != null) {
                    pendingRequestId.setValue(null);
                    navigateToMatch.setValue(matchId);
                }
            } else if ("DECLINED".equals(status) || "CANCELLED".equals(status)) {
                stopAutoDeclineTimer();
                pendingRequestId.setValue(null);
                errorMessage.setValue("Zahtev je odbijen.");
            }
        });
    }

    private void startAutoDeclineTimer(String requestId) {
        stopAutoDeclineTimer();
        autoDeclineTimer = new CountDownTimer(10000, 1000) {
            @Override public void onTick(long ms) {}
            @Override public void onFinish() {
                requestRepo.updateStatus(requestId, "DECLINED");
                incomingRequest.setValue(null);
                pendingRequestId.setValue(null);
            }
        }.start();
    }

    private void stopAutoDeclineTimer() {
        if (autoDeclineTimer != null) { autoDeclineTimer.cancel(); autoDeclineTimer = null; }
    }

    private Friend documentToFriend(String uid, DocumentSnapshot doc) {
        Friend f = new Friend();
        f.setUid(uid);
        f.setUsername(doc.getString("username"));
        f.setEmail(doc.getString("email"));
        Long starsVal    = doc.getLong("stars");
        Long leagueVal   = doc.getLong("league");
        Long avatarVal   = doc.getLong("avatarColorIndex");
        Boolean onlineVal = doc.getBoolean("online");
        Boolean inGameVal = doc.getBoolean("inGame");
        f.setStars(starsVal   != null ? starsVal.intValue()   : 0);
        f.setLeague(leagueVal != null ? leagueVal.intValue()  : 0);
        f.setAvatarColorIndex(avatarVal != null ? avatarVal.intValue() : 0);
        f.setOnline(Boolean.TRUE.equals(onlineVal));
        f.setInGame(Boolean.TRUE.equals(inGameVal));
        return f;
    }

    private void calculateMonthlyRanks(List<Friend> friendList) {
        if (friendList.isEmpty()) { friends.setValue(friendList); return; }

        java.util.Set<String> friendUids = new java.util.HashSet<>();
        for (Friend f : friendList) friendUids.add(f.getUid());

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .orderBy("monthlyStars",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.Map<String, Integer> rankMap = new java.util.HashMap<>();
                    int rank = 1;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String uid = doc.getId();
                        if (friendUids.contains(uid)) {
                            rankMap.put(uid, rank);
                        }
                        rank++;
                    }
                    for (Friend f : friendList) {
                        Integer r = rankMap.get(f.getUid());
                        f.setMonthlyRank(r != null ? r : 0);
                    }
                    friends.setValue(friendList);
                })
                .addOnFailureListener(e -> friends.setValue(friendList));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopAutoDeclineTimer();
        if (outgoingListener != null) outgoingListener.remove();

    }
}