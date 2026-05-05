package com.example.slagalica.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.slagalica.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class TopBarFragment extends Fragment {

    private TextView tvTokens, tvStars, tvLeague;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_top_bar, container, false);
        tvTokens = view.findViewById(R.id.tvTokens);
        tvStars = view.findViewById(R.id.tvStars);
        tvLeague = view.findViewById(R.id.tvLeague);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .addSnapshotListener((value, error) -> {
                        if (error != null || value == null || !value.exists()) {
                            return;
                        }

                        Long tokens = value.getLong("tokens");
                        Long stars = value.getLong("stars");
                        Long leagueIndex = value.getLong("league");

                        if (tokens != null) tvTokens.setText(String.valueOf(tokens));
                        if (stars != null) tvStars.setText(String.valueOf(stars));

                        if (leagueIndex != null) {
                            String leagueName = getLeagueName(leagueIndex.intValue());
                            tvLeague.setText(leagueName);
                        }
                    });
        }
    }

    private String getLeagueName(int index) {
        switch (index) {
            case 0: return "Početna";
            case 1: return "Bronzana";
            case 2: return "Srebrna";
            case 3: return "Zlatna";
            case 4: return "Platinasta";
            case 5: return "Dijamantska";
            default: return "Početna";
        }
    }
}
