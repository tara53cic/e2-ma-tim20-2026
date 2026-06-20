package com.example.slagalica.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.domain.models.UserGameStats;

public class StatisticsFragment extends Fragment {

    private StatisticsViewModel viewModel;

    // Opšta
    private TextView tvWinPercent, tvTotalGames, tvWinRate, tvLossRate;
    private ProgressBar pbWinRate, pbLossRate;

    // Ko zna zna
    private TextView tvKzzAvg, tvKzzCorrect, tvKzzIncorrect;
    private ProgressBar pbKzzCorrect, pbKzzIncorrect;

    // Spojnice
    private TextView tvSpojniceAvg, tvSpojniceRate, tvSpojniceFail;
    private ProgressBar pbSpojniceRate, pbSpojniceFail;

    // Asocijacije
    private TextView tvAsocijacijeAvg, tvAsocijacijeSolved, tvAsocijacijeUnsolved;
    private ProgressBar pbAsocijacijeSolved, pbAsocijacijeUnsolved;

    // Skočko
    private TextView tvSkockoAvg;
    private ProgressBar[] pbSkockoAt = new ProgressBar[6];
    private TextView[]    tvSkockoAt = new TextView[6];
    private ProgressBar pbSkockoFail;
    private TextView    tvSkockoFail;

    // Korak po korak
    private TextView tvKpkAvg;
    private ProgressBar[] pbKpkAt = new ProgressBar[7];
    private TextView[]    tvKpkAt = new TextView[7];
    private ProgressBar pbKpkFail;
    private TextView    tvKpkFail;

    // Moj broj
    private TextView tvMojBrojAvg, tvMojBrojRate, tvMojBrojFail;
    private ProgressBar pbMojBrojRate, pbMojBrojFail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
        bindViews(view);

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
        });

        viewModel.getStats().observe(getViewLifecycleOwner(), this::populateStats);
    }

    private void bindViews(View v) {
        // Opšta
        tvWinPercent = v.findViewById(R.id.tvWinPercent);
        tvTotalGames = v.findViewById(R.id.tvTotalGames);
        tvWinRate    = v.findViewById(R.id.tvWinRate);
        tvLossRate   = v.findViewById(R.id.tvLossRate);
        pbWinRate    = v.findViewById(R.id.pbWinRate);
        pbLossRate   = v.findViewById(R.id.pbLossRate);

        // Ko zna zna
        tvKzzAvg       = v.findViewById(R.id.tvKzzAvg);
        tvKzzCorrect   = v.findViewById(R.id.tvKzzCorrect);
        tvKzzIncorrect = v.findViewById(R.id.tvKzzIncorrect);
        pbKzzCorrect   = v.findViewById(R.id.pbKzzCorrect);
        pbKzzIncorrect = v.findViewById(R.id.pbKzzIncorrect);

        // Spojnice
        tvSpojniceAvg  = v.findViewById(R.id.tvSpojniceAvg);
        tvSpojniceRate = v.findViewById(R.id.tvSpojniceRate);
        tvSpojniceFail = v.findViewById(R.id.tvSpojniceFail);
        pbSpojniceRate = v.findViewById(R.id.pbSpojniceRate);
        pbSpojniceFail = v.findViewById(R.id.pbSpojniceFail);

        // Asocijacije
        tvAsocijacijeAvg      = v.findViewById(R.id.tvAsocijacijeAvg);
        tvAsocijacijeSolved   = v.findViewById(R.id.tvAsocijacijeSolved);
        tvAsocijacijeUnsolved = v.findViewById(R.id.tvAsocijacijeUnsolved);
        pbAsocijacijeSolved   = v.findViewById(R.id.pbAsocijacijeSolved);
        pbAsocijacijeUnsolved = v.findViewById(R.id.pbAsocijacijeUnsolved);

        // Skočko
        tvSkockoAvg = v.findViewById(R.id.tvSkockoAvg);
        int[] pbSkockoIds = {R.id.pbSkockoAt0, R.id.pbSkockoAt1, R.id.pbSkockoAt2,
                R.id.pbSkockoAt3, R.id.pbSkockoAt4, R.id.pbSkockoAt5};
        int[] tvSkockoIds = {R.id.tvSkockoAt0, R.id.tvSkockoAt1, R.id.tvSkockoAt2,
                R.id.tvSkockoAt3, R.id.tvSkockoAt4, R.id.tvSkockoAt5};
        for (int i = 0; i < 6; i++) {
            pbSkockoAt[i] = v.findViewById(pbSkockoIds[i]);
            tvSkockoAt[i] = v.findViewById(tvSkockoIds[i]);
        }
        pbSkockoFail = v.findViewById(R.id.pbSkockoFail);
        tvSkockoFail = v.findViewById(R.id.tvSkockoFail);

        // Korak po korak
        tvKpkAvg = v.findViewById(R.id.tvKpkAvg);
        int[] pbKpkIds = {R.id.pbKpkAt0, R.id.pbKpkAt1, R.id.pbKpkAt2, R.id.pbKpkAt3,
                R.id.pbKpkAt4, R.id.pbKpkAt5, R.id.pbKpkAt6};
        int[] tvKpkIds = {R.id.tvKpkAt0, R.id.tvKpkAt1, R.id.tvKpkAt2, R.id.tvKpkAt3,
                R.id.tvKpkAt4, R.id.tvKpkAt5, R.id.tvKpkAt6};
        for (int i = 0; i < 7; i++) {
            pbKpkAt[i] = v.findViewById(pbKpkIds[i]);
            tvKpkAt[i] = v.findViewById(tvKpkIds[i]);
        }
        pbKpkFail = v.findViewById(R.id.pbKpkFail);
        tvKpkFail = v.findViewById(R.id.tvKpkFail);

        // Moj broj
        tvMojBrojAvg  = v.findViewById(R.id.tvMojBrojAvg);
        tvMojBrojRate = v.findViewById(R.id.tvMojBrojRate);
        tvMojBrojFail = v.findViewById(R.id.tvMojBrojFail);
        pbMojBrojRate = v.findViewById(R.id.pbMojBrojRate);
        pbMojBrojFail = v.findViewById(R.id.pbMojBrojFail);
    }

    private void populateStats(UserGameStats s) {
        if (s == null) return;

        // Opšta statistika
        int total  = s.getTotalMatches();
        int wins   = s.getWins();
        int losses = s.getLosses();

        tvTotalGames.setText(String.valueOf(total));
        int winPct  = total > 0 ? Math.round(wins  * 100f / total) : 0;
        int lossPct = total > 0 ? Math.round(losses * 100f / total) : 0;

        tvWinPercent.setText(winPct + "%");
        tvWinRate.setText(winPct + "%");
        tvLossRate.setText(lossPct + "%");
        pbWinRate.setProgress(winPct);
        pbLossRate.setProgress(lossPct);

        // Ko zna zna
        int kzzGames     = s.getKzzGames();
        int kzzCorrect   = s.getKzzCorrect();
        int kzzIncorrect = s.getKzzIncorrect();
        int kzzTotal     = s.getKzzTotalQuestions();

        tvKzzAvg.setText(kzzGames > 0
                ? formatAvg(s.getKzzTotalPoints(), kzzGames, -25, 50) : "0 (-25 - 50)");

        int kzzCorrectPct   = kzzTotal > 0 ? Math.round(kzzCorrect   * 100f / kzzTotal) : 0;
        int kzzIncorrectPct = kzzTotal > 0 ? Math.round(kzzIncorrect * 100f / kzzTotal) : 0;
        tvKzzCorrect.setText(kzzCorrectPct + "%");
        tvKzzIncorrect.setText(kzzIncorrectPct + "%");
        pbKzzCorrect.setProgress(kzzCorrectPct);
        pbKzzIncorrect.setProgress(kzzIncorrectPct);

        // Spojnice
        int spojniceGames     = s.getSpojniceGames();
        int spojniceTotal     = s.getSpojniceTotalPairs();
        int spojniceConnected = s.getSpojniceConnected();

        tvSpojniceAvg.setText(spojniceGames > 0
                ? formatAvg(s.getSpojniceTotalPoints(), spojniceGames, 0, 10) : "0 (0 - 10)");

        int spojniceSuccessPct = spojniceTotal > 0
                ? Math.round(spojniceConnected * 100f / spojniceTotal) : 0;
        int spojniceFailPct    = 100 - spojniceSuccessPct;
        tvSpojniceRate.setText(spojniceSuccessPct + "%");
        tvSpojniceFail.setText(spojniceFailPct + "%");
        pbSpojniceRate.setProgress(spojniceSuccessPct);
        pbSpojniceFail.setProgress(spojniceFailPct);

        // Asocijacije
        int asocijacijeGames  = s.getAsocijacijeGames();
        int asocijacijeTotal  = s.getAsocijacijeTotal();
        int asocijacijeSolved = s.getAsocijacijeSolved();

        tvAsocijacijeAvg.setText(asocijacijeGames > 0
                ? formatAvg(s.getAsocijacijeTotalPoints(), asocijacijeGames, 0, 30) : "0 (0 - 30)");

        int asocijacijeSolvedPct   = asocijacijeTotal > 0
                ? Math.round(asocijacijeSolved * 100f / asocijacijeTotal) : 0;
        int asocijacijeUnsolvedPct = 100 - asocijacijeSolvedPct;
        tvAsocijacijeSolved.setText(asocijacijeSolvedPct + "%");
        tvAsocijacijeUnsolved.setText(asocijacijeUnsolvedPct + "%");
        pbAsocijacijeSolved.setProgress(asocijacijeSolvedPct);
        pbAsocijacijeUnsolved.setProgress(asocijacijeUnsolvedPct);

        // Skočko
        int skockoGames = s.getSkockoGames();
        int[] skockoAttempts = {
                s.getSkockoSolvedAt0(), s.getSkockoSolvedAt1(),
                s.getSkockoSolvedAt2(), s.getSkockoSolvedAt3(),
                s.getSkockoSolvedAt4(), s.getSkockoSolvedAt5()
        };

        tvSkockoAvg.setText(skockoGames > 0
                ? formatAvg(s.getSkockoTotalPoints(), skockoGames, 0, 20) : "0 (0 - 20)");

        int skockoSolvedTotal = 0;
        for (int val : skockoAttempts) skockoSolvedTotal += val;

        for (int i = 0; i < 6; i++) {
            int pct = skockoGames > 0 ? Math.round(skockoAttempts[i] * 100f / skockoGames) : 0;
            pbSkockoAt[i].setProgress(pct);
            tvSkockoAt[i].setText(pct + "%");
        }

        int skockoFailCount = skockoGames - skockoSolvedTotal;
        int skockoFailPct   = skockoGames > 0 ? Math.round(Math.max(0, skockoFailCount) * 100f / skockoGames) : 0;
        pbSkockoFail.setProgress(skockoFailPct);
        tvSkockoFail.setText(skockoFailPct + "%");

        // Korak po korak
        int kpkGames = s.getKpkGames();
        int[] kpkSteps = {
                s.getKpkSolvedAt0(), s.getKpkSolvedAt1(),
                s.getKpkSolvedAt2(), s.getKpkSolvedAt3(),
                s.getKpkSolvedAt4(), s.getKpkSolvedAt5(),
                s.getKpkSolvedAt6()
        };

        tvKpkAvg.setText(kpkGames > 0
                ? formatAvg(s.getKpkTotalPoints(), kpkGames, 0, 20) : "0 (0 - 20)");

        int kpkSolvedTotal = 0;
        for (int val : kpkSteps) kpkSolvedTotal += val;

        for (int i = 0; i < 7; i++) {
            int pct = kpkGames > 0 ? Math.round(kpkSteps[i] * 100f / kpkGames) : 0;
            pbKpkAt[i].setProgress(pct);
            tvKpkAt[i].setText(pct + "%");
        }

        int kpkFailCount = kpkGames - kpkSolvedTotal;
        int kpkFailPct   = kpkGames > 0 ? Math.round(Math.max(0, kpkFailCount) * 100f / kpkGames) : 0;
        pbKpkFail.setProgress(kpkFailPct);
        tvKpkFail.setText(kpkFailPct + "%");

        // Moj broj
        int mojBrojGames = s.getMojBrojGames();
        int mojBrojExact = s.getMojBrojExact();

        tvMojBrojAvg.setText(mojBrojGames > 0
                ? formatAvg(s.getMojBrojTotalPoints(), mojBrojGames, 0, 10) : "0 (0 - 10)");

        int mojBrojExactPct = mojBrojGames > 0
                ? Math.round(mojBrojExact * 100f / mojBrojGames) : 0;
        int mojBrojFailPct  = 100 - mojBrojExactPct;
        tvMojBrojRate.setText(mojBrojExactPct + "%");
        tvMojBrojFail.setText(mojBrojFailPct + "%");
        pbMojBrojRate.setProgress(mojBrojExactPct);
        pbMojBrojFail.setProgress(mojBrojFailPct);
    }

    private String formatAvg(int totalPoints, int games, int minPossible, int maxPossible) {
        int avg = Math.round((float) totalPoints / games);
        avg = Math.max(minPossible, Math.min(maxPossible, avg));
        return avg + " (" + minPossible + " - " + maxPossible + ")";
    }
}