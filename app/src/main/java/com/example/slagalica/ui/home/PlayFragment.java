package com.example.slagalica.ui.home;

import android.content.Intent;
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
import com.example.slagalica.domain.models.Match;
import com.example.slagalica.ui.match.MatchFragment;
import android.widget.Toast;

public class PlayFragment extends Fragment {

    private MatchRepository matchRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_play, container, false);

        matchRepository = new MatchRepository();

        Button btnStartGame = view.findViewById(R.id.btnStartGame);
        btnStartGame.setOnClickListener(v -> {
            String currentUserId = matchRepository.getCurrentUserId();
            if (currentUserId == null) {
                Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
                return;
            }

            Match newMatch = new Match(
                    null,
                    currentUserId,
                    null,
                    0,
                    0,
                    "IN_PROGRESS"
            );

            btnStartGame.setEnabled(false);

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
        });

        return view;
    }
}
