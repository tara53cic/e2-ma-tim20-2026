package com.example.slagalica.ui.match.associations;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.ui.match.MatchViewModel;

public class AssociationsFragment extends Fragment {

    private MatchViewModel sharedViewModel;
    private AssociationsViewModel associationsViewModel;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Button[][] fieldButtons;

    private EditText etSolutionA;
    private EditText etSolutionB;
    private EditText etSolutionC;
    private EditText etSolutionD;
    private EditText etFinalSolution;

    private Button btnSubmitAnswer;

    private static final int COLOR_OPENED = Color.parseColor("#F4A261");
    private static final int COLOR_SOLVED = Color.parseColor("#2EC27E");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_associations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);
        associationsViewModel = new ViewModelProvider(this).get(AssociationsViewModel.class);

        connectViews(view);
        setupFieldClicks();
        lockAllInputs();

        btnSubmitAnswer.setOnClickListener(v -> checkAnswer());

        sharedViewModel.startRoundTimer(120, this::finishRound);
    }
    private void connectViews(View view) {
        fieldButtons = new Button[][]{
                {
                        view.findViewById(R.id.btnA1),
                        view.findViewById(R.id.btnA2),
                        view.findViewById(R.id.btnA3),
                        view.findViewById(R.id.btnA4)
                },
                {
                        view.findViewById(R.id.btnB1),
                        view.findViewById(R.id.btnB2),
                        view.findViewById(R.id.btnB3),
                        view.findViewById(R.id.btnB4)
                },
                {
                        view.findViewById(R.id.btnC1),
                        view.findViewById(R.id.btnC2),
                        view.findViewById(R.id.btnC3),
                        view.findViewById(R.id.btnC4)
                },
                {
                        view.findViewById(R.id.btnD1),
                        view.findViewById(R.id.btnD2),
                        view.findViewById(R.id.btnD3),
                        view.findViewById(R.id.btnD4)
                }
        };

        etSolutionA = view.findViewById(R.id.etSolutionA);
        etSolutionB = view.findViewById(R.id.etSolutionB);
        etSolutionC = view.findViewById(R.id.etSolutionC);
        etSolutionD = view.findViewById(R.id.etSolutionD);
        etFinalSolution = view.findViewById(R.id.etFinalSolution);

        btnSubmitAnswer = view.findViewById(R.id.btnSubmitAnswer);
    }

    private void setupFieldClicks() {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                int c = column;
                int r = row;

                fieldButtons[c][r].setOnClickListener(v -> openField(c, r));
            }
        }
    }

    private void openField(int column, int row) {
        boolean opened = associationsViewModel.openField(column, row);

        if (!opened) return;

        fieldButtons[column][row].setText(associationsViewModel.fields[column][row]);
        fieldButtons[column][row].setEnabled(false);
        fieldButtons[column][row].setBackgroundTintList(ColorStateList.valueOf(COLOR_OPENED));

        unlockOnlyThisColumnAndFinal(column);
    }

    private void checkAnswer() {
        if (associationsViewModel.finalSolved) return;

        if (associationsViewModel.activeColumn == -1) {
            Toast.makeText(requireContext(), "Prvo otvorite neko polje.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEmpty(etFinalSolution)) {
            checkFinalAnswer();
            return;
        }

        if (!isEmpty(etSolutionA) && etSolutionA.isEnabled()) {
            checkColumnAnswer(0, etSolutionA);
            return;
        }

        if (!isEmpty(etSolutionB) && etSolutionB.isEnabled()) {
            checkColumnAnswer(1, etSolutionB);
            return;
        }

        if (!isEmpty(etSolutionC) && etSolutionC.isEnabled()) {
            checkColumnAnswer(2, etSolutionC);
            return;
        }

        if (!isEmpty(etSolutionD) && etSolutionD.isEnabled()) {
            checkColumnAnswer(3, etSolutionD);
            return;
        }

        Toast.makeText(requireContext(), "Unesite rešenje kolone ili konačno rešenje.", Toast.LENGTH_SHORT).show();
    }

    private void checkColumnAnswer(int column, EditText editText) {
        String answer = editText.getText().toString();

        if (associationsViewModel.checkColumnAnswer(column, answer)) {
            int points = associationsViewModel.solveColumn(column);

            editText.setText(associationsViewModel.columnSolutions[column]);
            editText.setTextColor(Color.WHITE);
            editText.setAlpha(1f);
            editText.setEnabled(false);
            editText.setBackgroundTintList(ColorStateList.valueOf(COLOR_SOLVED));

            revealColumn(column);

            Toast.makeText(requireContext(), "Tačno! +" + points + " bodova", Toast.LENGTH_SHORT).show();

            unlockUnsolvedColumnsAndFinal();
        } else {
            wrongAnswer();
        }
    }

    private void checkFinalAnswer() {
        String answer = etFinalSolution.getText().toString();

        if (associationsViewModel.checkFinalAnswer(answer)) {
            int points = associationsViewModel.solveFinal();

            etFinalSolution.setText(associationsViewModel.finalSolution);
            etFinalSolution.setEnabled(false);
            etFinalSolution.setBackgroundTintList(ColorStateList.valueOf(COLOR_SOLVED));

            revealAll();

            Toast.makeText(requireContext(), "Tačno konačno rešenje! +" + points + " bodova", Toast.LENGTH_SHORT).show();

            finishRound();
        } else {
            wrongAnswer();
        }
    }

    private void revealColumn(int column) {
        for (int row = 0; row < 4; row++) {
            fieldButtons[column][row].setText(associationsViewModel.fields[column][row]);
            fieldButtons[column][row].setEnabled(false);
            fieldButtons[column][row].setBackgroundTintList(ColorStateList.valueOf(COLOR_SOLVED));
        }
    }

    private void revealAll() {
        EditText[] solutionInputs = {
                etSolutionA,
                etSolutionB,
                etSolutionC,
                etSolutionD
        };

        for (int column = 0; column < 4; column++) {
            solutionInputs[column].setText(associationsViewModel.columnSolutions[column]);
            solutionInputs[column].setEnabled(false);
            solutionInputs[column].setBackgroundTintList(ColorStateList.valueOf(COLOR_SOLVED));

            revealColumn(column);
        }
    }

    private void lockAllInputs() {
        etSolutionA.setEnabled(false);
        etSolutionB.setEnabled(false);
        etSolutionC.setEnabled(false);
        etSolutionD.setEnabled(false);
        etFinalSolution.setEnabled(false);
    }

    private void unlockOnlyThisColumnAndFinal(int column) {
        lockAllInputs();

        if (column == 0 && !associationsViewModel.solvedColumns[0]) {
            etSolutionA.setEnabled(true);
        }

        if (column == 1 && !associationsViewModel.solvedColumns[1]) {
            etSolutionB.setEnabled(true);
        }

        if (column == 2 && !associationsViewModel.solvedColumns[2]) {
            etSolutionC.setEnabled(true);
        }

        if (column == 3 && !associationsViewModel.solvedColumns[3]) {
            etSolutionD.setEnabled(true);
        }

        etFinalSolution.setEnabled(true);
    }

    private void unlockUnsolvedColumnsAndFinal() {
        lockAllInputs();

        if (!associationsViewModel.solvedColumns[0]) {
            etSolutionA.setEnabled(true);
        }

        if (!associationsViewModel.solvedColumns[1]) {
            etSolutionB.setEnabled(true);
        }

        if (!associationsViewModel.solvedColumns[2]) {
            etSolutionC.setEnabled(true);
        }

        if (!associationsViewModel.solvedColumns[3]) {
            etSolutionD.setEnabled(true);
        }

        etFinalSolution.setEnabled(true);
    }

    private boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().isEmpty();
    }

    private void finishRound() {
        if (!isAdded()) return;

        sharedViewModel.stopTimer();
        sharedViewModel.addCurrentPlayerPoints(associationsViewModel.currentScore);

        handler.postDelayed(() -> {
            if (isAdded()) {
                sharedViewModel.advanceGamePhase();
            }
        }, 1000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }

    private void wrongAnswer() {
        Toast.makeText(
                requireContext(),
                "Nije tačno. Igra igrač " + (associationsViewModel.currentPlayer == 1 ? 2 : 1),
                Toast.LENGTH_SHORT
        ).show();

        clearInputs();
        lockAllInputs();

        associationsViewModel.switchPlayer();

        sharedViewModel.stopTimer();
        sharedViewModel.startRoundTimer(120, this::wrongAnswer);
    }

    private void clearInputs() {
        if (!associationsViewModel.solvedColumns[0]) {
            etSolutionA.setText("");
        }

        if (!associationsViewModel.solvedColumns[1]) {
            etSolutionB.setText("");
        }

        if (!associationsViewModel.solvedColumns[2]) {
            etSolutionC.setText("");
        }

        if (!associationsViewModel.solvedColumns[3]) {
            etSolutionD.setText("");
        }

        if (!associationsViewModel.finalSolved) {
            etFinalSolution.setText("");
        }
    }
}