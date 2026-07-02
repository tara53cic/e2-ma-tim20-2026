package com.example.slagalica.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.slagalica.R;
import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.domain.models.Match;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;

public class PlayFragment extends Fragment {

    private MatchRepository matchRepository;
    private UserRepository userRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_play, container, false);

        matchRepository = new MatchRepository();
        userRepository = new UserRepository();

        Button btnStartGame = view.findViewById(R.id.btnStartGame);
        btnStartGame.setOnClickListener(v -> {
            String currentUserId = matchRepository.getCurrentUserId();
            if (currentUserId == null) {
                Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
                return;
            }


            userRepository.getUser(currentUserId).addOnSuccessListener(doc -> {
                if (!doc.exists()) {
                    Toast.makeText(getContext(), "User data not found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Long tokensLong = doc.getLong("tokens");
                int tokens = tokensLong != null ? tokensLong.intValue() : 0;

                if (tokens <= 0) {
                    Toast.makeText(getContext(), "Nemate dostupnih tokena!\nTokene dobijate svakog dana.", Toast.LENGTH_SHORT).show();
                    return;
                }


                startGameMatching(btnStartGame, currentUserId, view);
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error checking tokens!", Toast.LENGTH_SHORT).show();
            });
        });

        return view;
    }

    private void startGameMatching(Button btnStartGame, String currentUserId, View view) {
        btnStartGame.setEnabled(false);

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
                        btnStartGame.setEnabled(true);
                        if (task.isSuccessful()) {
                            Bundle args = new Bundle();
                            args.putString("MATCH_ID", matchId);
                            androidx.navigation.Navigation.findNavController(view).navigate(R.id.action_playFragment_to_matchFragment, args);
                        } else {
                            Toast.makeText(getContext(), "Failed to join match", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Match newMatch = new Match(null, currentUserId, null, 0, 0, "WAITING");
                    matchRepository.createMatch(newMatch).addOnCompleteListener(task -> {
                        btnStartGame.setEnabled(true);
                        if (task.isSuccessful()) {
                            Bundle args = new Bundle();
                            args.putString("MATCH_ID", newMatch.getId());
                            androidx.navigation.Navigation.findNavController(view).navigate(R.id.action_playFragment_to_matchFragment, args);
                        } else {
                            Toast.makeText(getContext(), "Failed to create match", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnFailureListener(e -> {
                btnStartGame.setEnabled(true);
                Toast.makeText(getContext(), "Failed to find match", Toast.LENGTH_SHORT).show();
            });
        });
    }
}
