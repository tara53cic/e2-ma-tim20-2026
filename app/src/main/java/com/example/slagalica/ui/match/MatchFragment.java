package com.example.slagalica.ui.match;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.slagalica.R;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.ui.match.match_result.MatchResultFragment;
import com.example.slagalica.ui.match.number_game.NumberGameFragment;
import com.example.slagalica.ui.match.step_by_step.StepByStepFragment;
import com.example.slagalica.ui.match.who_knows.WhoKnowsFragment;
import com.example.slagalica.ui.match.matching.MatchingFragment;

public class MatchFragment extends Fragment {

    private MatchViewModel matchViewModel;
    private UserRepository userRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = new UserRepository();

        TextView tvTimer = view.findViewById(R.id.tvTimer);
        TextView tvPlayer1Score = view.findViewById(R.id.tvPlayer1Score);
        TextView tvPlayer2Score = view.findViewById(R.id.tvPlayer2Score);
        TextView tvPlayer1Name = view.findViewById(R.id.tvPlayer1Name);
        TextView tvPlayer2Name = view.findViewById(R.id.tvPlayer2Name);

        matchViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);

        if (getArguments() != null) {
            String matchId = getArguments().getString("MATCH_ID");
            if (matchId != null) {
                matchViewModel.initMatch(matchId);
            }
        }

        matchViewModel.getTimeRemaining().observe(getViewLifecycleOwner(), time -> tvTimer.setText(String.valueOf(time)));

        matchViewModel.getPlayer1Score().observe(getViewLifecycleOwner(), score -> tvPlayer1Score.setText(String.valueOf(score)));
        matchViewModel.getPlayer2Score().observe(getViewLifecycleOwner(), score -> tvPlayer2Score.setText(String.valueOf(score)));

        matchViewModel.getPlayer1Name().observe(getViewLifecycleOwner(), tvPlayer1Name::setText);
        matchViewModel.getPlayer2Name().observe(getViewLifecycleOwner(), tvPlayer2Name::setText);

        matchViewModel.getCurrentFragment().observe(getViewLifecycleOwner(), fragmentName -> {
            Fragment fragment;
            if ("KZZ".equals(fragmentName)) {
                fragment = new WhoKnowsFragment();
            } else if ("SPOJNICE_R1".equals(fragmentName) || "SPOJNICE_R2".equals(fragmentName)) {
                fragment = new MatchingFragment();
            } else if ("MOJ_BROJ_R1".equals(fragmentName) || "MOJ_BROJ_R2".equals(fragmentName)) {
                fragment = new NumberGameFragment();
            } else if (fragmentName != null && fragmentName.startsWith("KORAK_PO_KORAK")) {
                fragment = new StepByStepFragment();
            } else if ("FINISHED".equals(fragmentName)) {
                fragment = new MatchResultFragment();
            } else {
                Navigation.findNavController(view).popBackStack();
                return;
            }
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.game_fragment_container, fragment)
                    .commit();
        });

        if (savedInstanceState == null) {
            userRepository.deductTokens(5);
        }

        View bottomNav = requireActivity().findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View bottomNav = requireActivity().findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
        }
    }
}
