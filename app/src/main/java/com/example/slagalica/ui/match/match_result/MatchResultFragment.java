package com.example.slagalica.ui.match.match_result;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.data.UserStatsRepository;
import com.example.slagalica.ui.auth.AuthActivity;
import com.example.slagalica.ui.match.MatchViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MatchResultFragment extends Fragment {

    private MatchViewModel sharedViewModel;
    private MatchResultViewModel resultViewModel;
    private final UserStatsRepository statsRepo = new UserStatsRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);
        resultViewModel = new ViewModelProvider(this).get(MatchResultViewModel.class);

        TextView tvResultInfo = view.findViewById(R.id.tvResultInfo);
        TextView tvStarsWon = view.findViewById(R.id.tvStarsWon);
        TextView tvTokensWon = view.findViewById(R.id.tvTokensWon);
        MaterialButton btnHome = view.findViewById(R.id.btnHome);

        int p1Score = sharedViewModel.getPlayer1Score().getValue() != null ? sharedViewModel.getPlayer1Score().getValue() : 0;
        int p2Score = sharedViewModel.getPlayer2Score().getValue() != null ? sharedViewModel.getPlayer2Score().getValue() : 0;

        resultViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                tvResultInfo.setText(R.string.loading_results);
            }
        });

        resultViewModel.getStatsResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                tvResultInfo.setText(result.isWinner ? "Pobedili ste!" : "Izgubili ste!");
                tvStarsWon.setText((result.totalStarsChange > 0 ? "+" : "") + result.totalStarsChange);
                tvTokensWon.setText("+" + result.tokensAdded);

                String uid = statsRepo.getCurrentUid();
                if (uid != null) {
                    statsRepo.recordMatchResult(uid, result.isWinner);
                }
            }
        });

        boolean isPlayer1 = sharedViewModel.getIsPlayer1();
        String matchId = sharedViewModel.getMatchId();
        
        if (sharedViewModel.isChallenge()) {
            tvResultInfo.setText("Čekanje ostalih igrača...");
            tvStarsWon.setText("");
            tvTokensWon.setText("");
            sharedViewModel.getChallengeOutcome().observe(getViewLifecycleOwner(), outcome -> {
                if (outcome == null) return;
                String place = outcome.placement + ". mesto od " + outcome.totalPlayers;
                tvResultInfo.setText((outcome.isWinner ? "Pobedili ste izazov! " : "Izazov završen. ")
                        + "Poena: " + outcome.myScore + " (" + place + ")");
                tvStarsWon.setText((outcome.starsChange > 0 ? "+" : "") + outcome.starsChange);
                tvTokensWon.setText((outcome.tokensChange > 0 ? "+" : "") + outcome.tokensChange);
            });
        } else {
            resultViewModel.calculateAndSaveStats(isPlayer1, p1Score, p2Score, matchId, false, false);
        }

        btnHome.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && (currentUser.isAnonymous() || (currentUser.getEmail() != null && currentUser.getEmail().endsWith("@guest.slagalica.com")))) {
                String uid = currentUser.getUid();
                FirebaseFirestore.getInstance().collection("users").document(uid).delete().addOnCompleteListener(task -> {
                    currentUser.delete().addOnCompleteListener(task1 -> {
                        Intent intent = new Intent(requireActivity(), AuthActivity.class);
                        startActivity(intent);
                        requireActivity().finish();
                    });
                });
            } else {
                androidx.navigation.Navigation.findNavController(v).navigate(R.id.nav_play);
            }
        });

        if (sharedViewModel.getMatchId() != null) {
            FirebaseFirestore.getInstance().collection("matches")
                    .document(sharedViewModel.getMatchId())
                    .update("status", "FINISHED");
        }
    }
}