package com.example.slagalica.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.slagalica.MainActivity;
import com.example.slagalica.R;
import com.example.slagalica.data.AuthRepository;
import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.domain.models.Match;
import com.example.slagalica.domain.models.User;
import com.google.firebase.firestore.DocumentSnapshot;

import android.widget.Toast;
import java.util.UUID;

public class LaunchFragment extends Fragment {

    private AuthRepository authRepository;
    private MatchRepository matchRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        authRepository = new AuthRepository();
        matchRepository = new MatchRepository();
        return inflater.inflate(R.layout.fragment_launch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnLogin).setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_launchFragment_to_loginFragment);
        });

        view.findViewById(R.id.btnRegister).setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_launchFragment_to_registerFragment);
        });

        view.findViewById(R.id.btnGuest).setOnClickListener(v -> {
            v.setEnabled(false);

            String randomId = UUID.randomUUID().toString().substring(0, 8);
            String guestEmail = "guest_" + randomId + "@guest.slagalica.com";
            String guestPassword = "GuestPassword123!";

            authRepository.registerUser(guestEmail, guestPassword).addOnCompleteListener(task -> {
                v.setEnabled(true);
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                    String uid = task.getResult().getUser().getUid();
                    User guestUser = new User(guestEmail, "Gost", "Unknown");
                    guestUser.setTokens(0);
                    authRepository.saveUserToFirestore(uid, guestUser).addOnCompleteListener(saveTask -> {
                        if (saveTask.isSuccessful()) {
                            startDirectMatch(uid);
                        } else {
                            v.setEnabled(true);
                            Toast.makeText(getContext(), "Failed to create guest profile", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    v.setEnabled(true);
                    String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Toast.makeText(getContext(), "Failed to login as guest: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void startDirectMatch(String currentUserId) {
        matchRepository.deleteWaitingMatches(currentUserId).addOnCompleteListener(cleanupTask -> {
            matchRepository.findAvailableMatch().addOnSuccessListener(querySnapshot -> {
                DocumentSnapshot availableMatch = null;
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String p1Id = doc.getString("player1_id");
                    if (p1Id != null && !currentUserId.equals(p1Id) && doc.getString("player2_id") == null) {
                        availableMatch = doc;
                        break;
                    }
                }

                if (availableMatch != null) {
                    final String matchId = availableMatch.getId();
                    matchRepository.joinMatch(matchId, currentUserId).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            navigateToMatch(matchId);
                        } else {
                            Toast.makeText(getContext(), "Failed to join match", Toast.LENGTH_SHORT).show();
                            navigateToHome();
                        }
                    });
                } else {
                    Match newMatch = new Match(null, currentUserId, null, 0, 0, "WAITING");
                    matchRepository.createMatch(newMatch).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            navigateToMatch(newMatch.getId());
                        } else {
                            Toast.makeText(getContext(), "Failed to create match", Toast.LENGTH_SHORT).show();
                            navigateToHome();
                        }
                    });
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to find match", Toast.LENGTH_SHORT).show();
                navigateToHome();
            });
        });
    }

    private void navigateToMatch(String matchId) {
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.putExtra("NAVIGATE_TO", "MATCH");
        intent.putExtra("MATCH_ID", matchId);
        startActivity(intent);
        requireActivity().finish();
    }

    private void navigateToHome() {
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }
}
