package com.example.slagalica.ui.match.skocko;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.ui.match.MatchViewModel;

public class SkockoFragment extends Fragment {
    private SkockoViewModel viewModel;
    private TextView tvRoundInfo;
    private TextView[][] cells;
    private TextView[][] results;
    private TextView[] opponentCells;
    private TextView[] opponentResults;
    private Button btnSkocko, btnClub, btnSpade, btnHeart, btnTriangle, btnStar;
    private Button btnConfirm;
    private MatchViewModel sharedViewModel;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final int COLOR_CELL = Color.parseColor("#0F3356");
    private static final int COLOR_PLAYER_RESULT = Color.parseColor("#F4A261");
    private static final int COLOR_CORRECT_PLACE = Color.parseColor("#2EC27E");
    private static final int COLOR_WRONG_PLACE = Color.parseColor("#F4A261");
    private static final int COLOR_EMPTY_RESULT = Color.parseColor("#020617");
    private static final int COLOR_OPPONENT = Color.parseColor("#7C2D12");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_skocko, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SkockoViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);

        connectViews(view);
        setupCellClicks();
        setupSymbolButtons();
        setupConfirmButton();

        tvRoundInfo.setText("Tvoja partija - 6 pokušaja");
        startPlayerTimer();
    }

    private void setupCellClicks() {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                final int r = row;
                final int c = col;

                cells[r][c].setOnClickListener(v -> {
                    if (r == viewModel.currentAttempt && !viewModel.playerTurnFinished && !viewModel.roundFinished) {
                        removeSymbolFromCurrentAttempt(c);
                    }
                });
            }
        }
    }

    private void removeSymbolFromCurrentAttempt(int index) {
        if (index >= viewModel.currentInputIndex) return;

        boolean removed = viewModel.removeSymbolAt(index);
        if (!removed) return;

        int row = viewModel.currentAttempt;

        for (int col = 0; col < 4; col++) {
            cells[row][col].setText("");

            if (col < viewModel.currentInputIndex) {
                cells[row][col].setText(viewModel.currentInput[col]);
            }
        }
    }

    private void connectViews(View view) {
        tvRoundInfo = view.findViewById(R.id.tvRoundInfo);

        cells = new TextView[][]{
                {view.findViewById(R.id.cell00), view.findViewById(R.id.cell01), view.findViewById(R.id.cell02), view.findViewById(R.id.cell03)},
                {view.findViewById(R.id.cell10), view.findViewById(R.id.cell11), view.findViewById(R.id.cell12), view.findViewById(R.id.cell13)},
                {view.findViewById(R.id.cell20), view.findViewById(R.id.cell21), view.findViewById(R.id.cell22), view.findViewById(R.id.cell23)},
                {view.findViewById(R.id.cell30), view.findViewById(R.id.cell31), view.findViewById(R.id.cell32), view.findViewById(R.id.cell33)},
                {view.findViewById(R.id.cell40), view.findViewById(R.id.cell41), view.findViewById(R.id.cell42), view.findViewById(R.id.cell43)},
                {view.findViewById(R.id.cell50), view.findViewById(R.id.cell51), view.findViewById(R.id.cell52), view.findViewById(R.id.cell53)}
        };

        results = new TextView[][]{
                {view.findViewById(R.id.res00), view.findViewById(R.id.res01), view.findViewById(R.id.res02), view.findViewById(R.id.res03)},
                {view.findViewById(R.id.res10), view.findViewById(R.id.res11), view.findViewById(R.id.res12), view.findViewById(R.id.res13)},
                {view.findViewById(R.id.res20), view.findViewById(R.id.res21), view.findViewById(R.id.res22), view.findViewById(R.id.res23)},
                {view.findViewById(R.id.res30), view.findViewById(R.id.res31), view.findViewById(R.id.res32), view.findViewById(R.id.res33)},
                {view.findViewById(R.id.res40), view.findViewById(R.id.res41), view.findViewById(R.id.res42), view.findViewById(R.id.res43)},
                {view.findViewById(R.id.res50), view.findViewById(R.id.res51), view.findViewById(R.id.res52), view.findViewById(R.id.res53)}
        };

        opponentCells = new TextView[]{
                view.findViewById(R.id.opCell0),
                view.findViewById(R.id.opCell1),
                view.findViewById(R.id.opCell2),
                view.findViewById(R.id.opCell3)
        };

        opponentResults = new TextView[]{
                view.findViewById(R.id.opRes0),
                view.findViewById(R.id.opRes1),
                view.findViewById(R.id.opRes2),
                view.findViewById(R.id.opRes3)
        };

        btnSkocko = view.findViewById(R.id.btnSkocko);
        btnClub = view.findViewById(R.id.btnClub);
        btnSpade = view.findViewById(R.id.btnSpade);
        btnHeart = view.findViewById(R.id.btnHeart);
        btnTriangle = view.findViewById(R.id.btnTriangle);
        btnStar = view.findViewById(R.id.btnStar);
        btnConfirm = view.findViewById(R.id.btnConfirm);
    }

    private void setupSymbolButtons() {
        btnSkocko.setOnClickListener(v -> addSymbol("☻"));
        btnClub.setOnClickListener(v -> addSymbol("♣"));
        btnSpade.setOnClickListener(v -> addSymbol("♠"));
        btnHeart.setOnClickListener(v -> addSymbol("♥"));
        btnTriangle.setOnClickListener(v -> addSymbol("▲"));
        btnStar.setOnClickListener(v -> addSymbol("★"));
    }

    private void setupConfirmButton() {
        btnConfirm.setOnClickListener(v -> confirmPlayerAttempt());
    }

    private void addSymbol(String symbol) {
        if (viewModel.playerTurnFinished || viewModel.roundFinished) return;

        boolean added = viewModel.addSymbol(symbol);

        if (!added) return;

        int row = viewModel.currentAttempt;
        int column = viewModel.currentInputIndex - 1;

        cells[row][column].setText(symbol);
        cells[row][column].setTextColor(Color.WHITE);
        cells[row][column].setGravity(android.view.Gravity.CENTER);
    }

    private void confirmPlayerAttempt() {
        if (viewModel.playerTurnFinished || viewModel.roundFinished) return;

        if (!viewModel.isInputFull()) {
            Toast.makeText(requireContext(), "Unesi 4 znaka.", Toast.LENGTH_SHORT).show();
            return;
        }

        int[] result = viewModel.checkCurrentInput();
        showResult(results[viewModel.currentAttempt], result);

        if (viewModel.isSolved(result)) {
            int points = viewModel.calculatePlayerPoints();
            viewModel.playerScore += points;
            viewModel.roundFinished = true;

            stopTimer();
            tvRoundInfo.setText("Pogodila si! +" + points + " bodova");

            disableButtons();
            finishSkockoRound();
            return;
        }

        viewModel.currentAttempt++;
        viewModel.clearInput();

        if (viewModel.currentAttempt >= 6) {
            startOpponentAttempt();
        }
    }

    private void finishSkockoRound() {
        stopTimer();

        handler.postDelayed(() -> {
            if (isAdded()) {
                sharedViewModel.advanceGamePhase();
            }
        }, 1500);
    }

    private void startOpponentAttempt() {
        viewModel.playerTurnFinished = true;

        stopTimer();
        disableButtons();

        tvRoundInfo.setText("Sistem ima jedan pokušaj");

        handler.postDelayed(() -> {
            String[] systemGuess = viewModel.generateSystemGuess();

            for (int i = 0; i < 4; i++) {
                opponentCells[i].setText(systemGuess[i]);
                opponentCells[i].setTextColor(Color.WHITE);
                opponentCells[i].setTextSize(18);
                opponentCells[i].setGravity(android.view.Gravity.CENTER);
                opponentCells[i].setBackgroundTintList(ColorStateList.valueOf(COLOR_OPPONENT));
            }

            int[] result = viewModel.checkCombination(systemGuess);
            showResult(opponentResults, result);

            if (viewModel.isSolved(result)) {
                viewModel.systemScore += 10;
                tvRoundInfo.setText("Sistem je pogodio! Sistem +10");
            } else {
                tvRoundInfo.setText("Niko nije pogodio kombinaciju");
            }

            viewModel.opponentAttemptUsed = true;
            viewModel.roundFinished = true;

            finishSkockoRound();

        }, 1000);
    }

    private void showResult(TextView[] resultViews, int[] result) {
        int correctPlace = result[0];
        int wrongPlace = result[1];

        int index = 0;

        for (int i = 0; i < correctPlace; i++) {
            resultViews[index].setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT_PLACE));
            index++;
        }

        for (int i = 0; i < wrongPlace; i++) {
            resultViews[index].setBackgroundTintList(ColorStateList.valueOf(COLOR_WRONG_PLACE));
            index++;
        }

        while (index < 4) {
            resultViews[index].setBackgroundTintList(ColorStateList.valueOf(COLOR_EMPTY_RESULT));
            index++;
        }
    }

    private void startPlayerTimer() {
        sharedViewModel.startRoundTimer(30, () -> {
            if (!viewModel.roundFinished) {
                startOpponentAttempt();
            }
        });

        tvRoundInfo.setText("Tvoja partija - 6 pokušaja");
    }

    private void stopTimer() {
        sharedViewModel.stopTimer();
    }

    private void disableButtons() {
        btnSkocko.setEnabled(false);
        btnClub.setEnabled(false);
        btnSpade.setEnabled(false);
        btnHeart.setEnabled(false);
        btnTriangle.setEnabled(false);
        btnStar.setEnabled(false);
        btnConfirm.setEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimer();
        handler.removeCallbacksAndMessages(null);
    }
}