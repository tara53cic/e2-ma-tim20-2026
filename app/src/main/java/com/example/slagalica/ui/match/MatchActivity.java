package com.example.slagalica.ui.match;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.Fragment;

import com.example.slagalica.R;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.ui.match.match_result.MatchResultFragment;
import com.example.slagalica.ui.match.number_game.NumberGameFragment;
import com.example.slagalica.ui.match.step_by_step.StepByStepFragment;
import com.example.slagalica.ui.match.who_knows.WhoKnowsFragment;
import com.example.slagalica.ui.match.matching.MatchingFragment;

public class MatchActivity extends AppCompatActivity {

    private MatchViewModel matchViewModel;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        userRepository = new UserRepository();

        TextView tvTimer = findViewById(R.id.tvTimer);
        TextView tvPlayer1Score = findViewById(R.id.tvPlayer1Score);
        TextView tvPlayer2Score = findViewById(R.id.tvPlayer2Score);
        TextView tvPlayer1Name = findViewById(R.id.tvPlayer1Name);
        TextView tvPlayer2Name = findViewById(R.id.tvPlayer2Name);

        matchViewModel = new ViewModelProvider(this).get(MatchViewModel.class);

        String matchId = getIntent().getStringExtra("MATCH_ID");
        if (matchId != null) {
            matchViewModel.initMatch(matchId);
        }

        matchViewModel.getTimeRemaining().observe(this, time -> tvTimer.setText(String.valueOf(time)));

        matchViewModel.getPlayer1Score().observe(this, score -> tvPlayer1Score.setText(String.valueOf(score)));
        matchViewModel.getPlayer2Score().observe(this, score -> tvPlayer2Score.setText(String.valueOf(score)));

        matchViewModel.getPlayer1Name().observe(this, tvPlayer1Name::setText);
        matchViewModel.getPlayer2Name().observe(this, tvPlayer2Name::setText);

        matchViewModel.getCurrentFragment().observe(this, fragmentName -> {
            Fragment fragment;
            if ("KZZ".equals(fragmentName)) {
                fragment = new WhoKnowsFragment();
            } else if ("SPOJNICE_R1".equals(fragmentName) || "SPOJNICE_R2".equals(fragmentName)) {
                fragment = new MatchingFragment();
            } else if ("MOJ_BROJ_R1".equals(fragmentName) || "MOJ_BROJ_R2".equals(fragmentName)) {
                fragment = new NumberGameFragment();
            } else if (fragmentName.startsWith("KORAK_PO_KORAK")) {
                fragment = new StepByStepFragment();
            } else if ("FINISHED".equals(fragmentName)) {
                fragment = new MatchResultFragment();
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