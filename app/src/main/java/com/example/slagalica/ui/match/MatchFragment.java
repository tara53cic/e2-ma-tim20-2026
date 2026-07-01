package com.example.slagalica.ui.match;

import android.content.res.ColorStateList;
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
import com.example.slagalica.ui.match.associations.AssociationsFragment;
import com.example.slagalica.ui.match.match_result.MatchResultFragment;
import com.example.slagalica.ui.match.number_game.NumberGameFragment;
import com.example.slagalica.ui.match.skocko.SkockoFragment;
import com.example.slagalica.ui.match.step_by_step.StepByStepFragment;
import com.example.slagalica.ui.match.who_knows.WhoKnowsFragment;
import com.example.slagalica.ui.match.matching.MatchingFragment;
import com.google.android.material.imageview.ShapeableImageView;

public class MatchFragment extends Fragment {

    private static final int[] AVATAR_ICONS = {
            R.drawable.ic_avatar_1, R.drawable.ic_avatar_2,
            R.drawable.ic_avatar_3, R.drawable.ic_avatar_4,
            R.drawable.ic_avatar_5, R.drawable.ic_avatar_6
    };

    private static final int[] AVATAR_COLORS = {
            0xFF508EFA, 0xFF2EC27E, 0xFFE53935,
            0xFFF4A261, 0xFF9C27B0, 0xFF00BCD4
    };

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

        TextView tvTimer        = view.findViewById(R.id.tvTimer);
        TextView tvPlayer1Score = view.findViewById(R.id.tvPlayer1Score);
        TextView tvPlayer2Score = view.findViewById(R.id.tvPlayer2Score);
        TextView tvPlayer1Name  = view.findViewById(R.id.tvPlayer1Name);
        TextView tvPlayer2Name  = view.findViewById(R.id.tvPlayer2Name);
        ShapeableImageView ivPlayer1Avatar = view.findViewById(R.id.ivPlayer1Avatar);
        ShapeableImageView ivPlayer2Avatar = view.findViewById(R.id.ivPlayer2Avatar);

        matchViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);

        if (getArguments() != null) {
            String matchId = getArguments().getString("MATCH_ID");
            if (matchId != null) {
                matchViewModel.initMatch(matchId);
            } else {
                Navigation.findNavController(view).popBackStack();
                return;
            }
        } else {
            Navigation.findNavController(view).popBackStack();
            return;
        }

        matchViewModel.getTimeRemaining().observe(getViewLifecycleOwner(),
                time -> tvTimer.setText(String.valueOf(time)));

        matchViewModel.getPlayer1Score().observe(getViewLifecycleOwner(),
                score -> tvPlayer1Score.setText(String.valueOf(score)));
        matchViewModel.getPlayer2Score().observe(getViewLifecycleOwner(),
                score -> tvPlayer2Score.setText(String.valueOf(score)));

        matchViewModel.getPlayer1Name().observe(getViewLifecycleOwner(), tvPlayer1Name::setText);
        matchViewModel.getPlayer2Name().observe(getViewLifecycleOwner(), tvPlayer2Name::setText);

        // Avatari
        matchViewModel.getPlayer1AvatarIndex().observe(getViewLifecycleOwner(), idx -> {
            if (idx == null) return;
            int i = Math.max(0, Math.min(idx, AVATAR_ICONS.length - 1));
            ivPlayer1Avatar.setImageResource(AVATAR_ICONS[i]);
            ivPlayer1Avatar.setBackgroundTintList(ColorStateList.valueOf(AVATAR_COLORS[i]));
        });

        matchViewModel.getPlayer2AvatarIndex().observe(getViewLifecycleOwner(), idx -> {
            if (idx == null) return;
            int i = Math.max(0, Math.min(idx, AVATAR_ICONS.length - 1));
            ivPlayer2Avatar.setImageResource(AVATAR_ICONS[i]);
            ivPlayer2Avatar.setBackgroundTintList(ColorStateList.valueOf(AVATAR_COLORS[i]));
        });

        View waitingOverlay = view.findViewById(R.id.waitingOverlay);

        matchViewModel.getCurrentFragment().observe(getViewLifecycleOwner(), fragmentName -> {
            if ("WAITING".equals(fragmentName)) {
                waitingOverlay.setVisibility(View.VISIBLE);
                return;
            }
            waitingOverlay.setVisibility(View.GONE);

            Fragment fragment;
            if ("KZZ".equals(fragmentName)) {
                fragment = new WhoKnowsFragment();
            } else if ("SPOJNICE_R1".equals(fragmentName) || "SPOJNICE_R2".equals(fragmentName)) {
                fragment = new MatchingFragment();
            } else if ("ASOCIJACIJE_R1".equals(fragmentName) || "ASOCIJACIJE_R2".equals(fragmentName)) {
                fragment = new AssociationsFragment();
            } else if ("SKOCKO_R1".equals(fragmentName) || "SKOCKO_R2".equals(fragmentName)) {
                fragment = new SkockoFragment();
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

            matchViewModel.shouldDeductToken();
            userRepository.setInGame(true);
        }

        View bottomNav = requireActivity().findViewById(R.id.bottom_nav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        userRepository.setInGame(false);
        View bottomNav = requireActivity().findViewById(R.id.bottom_nav);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
    }
}