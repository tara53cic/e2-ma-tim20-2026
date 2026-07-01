package com.example.slagalica.ui.home;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.slagalica.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class TopBarFragment extends Fragment {

    private static final int[] LEAGUE_ICONS = {
            R.drawable.ic_league_0,
            R.drawable.ic_league_1,
            R.drawable.ic_league_2,
            R.drawable.ic_league_3,
            R.drawable.ic_league_4,
            R.drawable.ic_league_5
    };

    private static final String[] LEAGUE_NAMES = {
            "Početna", "Bronzana", "Srebrna", "Zlatna", "Platinasta", "Dijamantska"
    };

    private TextView tvTokens, tvStars, tvLeague;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_top_bar, container, false);
        tvTokens = view.findViewById(R.id.tvTokens);
        tvStars  = view.findViewById(R.id.tvStars);
        tvLeague = view.findViewById(R.id.tvLeague);
        return view;
    }

    private int previousLeague = -1;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !value.exists()) return;
                    if (!isAdded()) return;

                    Long tokens    = value.getLong("tokens");
                    Long stars     = value.getLong("stars");
                    Long leagueIdx = value.getLong("league");

                    if (tokens != null) tvTokens.setText(String.valueOf(Math.max(0, tokens)));
                    if (stars  != null) tvStars.setText(String.valueOf(stars));

                    if (leagueIdx != null) {
                        int index = (int) Math.max(0, Math.min(leagueIdx, LEAGUE_NAMES.length - 1));
                        tvLeague.setText(LEAGUE_NAMES[index]);

                        Drawable icon = ContextCompat.getDrawable(requireContext(), LEAGUE_ICONS[index]);
                        if (icon != null) {
                            icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                            tvLeague.setCompoundDrawables(icon, null, null, null);
                        }

                        if (previousLeague != -1 && index != previousLeague) {
                            showLeagueChangeDialog(index, previousLeague);
                        }
                        previousLeague = index;
                    }
                });
    }

    private void showLeagueChangeDialog(int newLeague, int oldLeague) {
        if (!isAdded() || getContext() == null) return;

        boolean promoted = newLeague > oldLeague;
        String title = promoted ? "🎉 Napredovanje u ligu!" : "📉 Pad u nižu ligu";
        String message = promoted
                ? "Čestitamo! Prešli ste u " + LEAGUE_NAMES[newLeague] + " ligu!"
                : "Pali ste na " + LEAGUE_NAMES[newLeague] + " ligu. Trudite se više!";

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setIcon(LEAGUE_ICONS[newLeague])
                .setPositiveButton("OK", null)
                .show();
    }
}