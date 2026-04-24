package com.example.slagalica.ui.game;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.Fragment;

import com.example.slagalica.R;
import com.example.slagalica.data.UserRepository;

public class GameActivity extends AppCompatActivity {

    private SharedGameViewModel sharedGameViewModel;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        userRepository = new UserRepository();

        TextView tvTimer = findViewById(R.id.tvTimer);
        TextView tvPlayer1Score = findViewById(R.id.tvPlayer1Score);
        TextView tvPlayer2Score = findViewById(R.id.tvPlayer2Score);

        sharedGameViewModel = new ViewModelProvider(this).get(SharedGameViewModel.class);

        sharedGameViewModel.getTimeRemaining().observe(this, time -> {
            tvTimer.setText(String.valueOf(time));
        });

        sharedGameViewModel.getPlayer1Score().observe(this, score -> {
            tvPlayer1Score.setText(String.valueOf(score));
        });

        sharedGameViewModel.getPlayer2Score().observe(this, score -> {
            tvPlayer2Score.setText(String.valueOf(score));
        });

        sharedGameViewModel.getCurrentFragment().observe(this, fragmentName -> {
            Fragment fragment;
            if ("MOJ_BROJ_R1".equals(fragmentName) || "MOJ_BROJ_R2".equals(fragmentName)) {
                fragment = new NumberGameFragment();
            } else if (fragmentName.startsWith("KORAK_PO_KORAK")) {
                fragment = new StepByStepFragment();
            } else {
                finish();
                return;
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.game_fragment_container, fragment)
                    .commit();
        });

        if (savedInstanceState == null) {
            userRepository.deductTokens(5);
        }
    }
}
