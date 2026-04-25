package com.example.slagalica.ui.match.step_by_step;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.domain.models.StepByStep;
import com.example.slagalica.ui.match.MatchViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class StepByStepFragment extends Fragment {

    private MatchViewModel sharedViewModel;
    private StepByStepViewModel stepByStepViewModel;

    private List<TextView> steps = new ArrayList<>();
    private TextInputEditText etStepAnswer;
    private MaterialButton btnConfirmStep;

    private List<String> currentHints = new ArrayList<>();
    private String currentAnswer = "";

    private int currentRevealedStep = 0;
    private boolean isOpponentPhase = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_step_by_step, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);
        stepByStepViewModel = new ViewModelProvider(this).get(StepByStepViewModel.class);

        ViewGroup stepsContainer = view.findViewById(R.id.stepsContainer);
        if (stepsContainer != null) {
            for (int i = 0; i < stepsContainer.getChildCount(); i++) {
                View child = stepsContainer.getChildAt(i);
                if (child instanceof TextView) {
                    steps.add((TextView) child);
                }
            }
        }

        etStepAnswer = view.findViewById(R.id.etStepAnswer);
        btnConfirmStep = view.findViewById(R.id.btnConfirmStep);

        stepByStepViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && !loading) {
                // Initialize based on round
                List<StepByStep> games = stepByStepViewModel.getGames().getValue();
                if (games != null && !games.isEmpty()) {
                    boolean isR1 = "KORAK_PO_KORAK_R1".equals(sharedViewModel.getCurrentFragment().getValue());
                    StepByStep currentData = isR1 ? games.get(0) : games.get((games.size() > 1) ? 1 : 0);

                    currentHints.clear();
                    for(int i = 1; i <= 7; i++) {
                        currentHints.add(currentData.getStep(i));
                    }
                    currentAnswer = currentData.getFinal_answer();
                    setupRound();
                } else {
                    Toast.makeText(getContext(), "Nije moguće učitati podatke!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupRound() {
        btnConfirmStep.setEnabled(true);
        btnConfirmStep.setOnClickListener(v -> checkAnswer());

        currentRevealedStep = 0;
        isOpponentPhase = false;

        sharedViewModel.getTimeRemaining().observe(getViewLifecycleOwner(), time -> {
            if (isOpponentPhase) {
                if (time == 0) {
                    sharedViewModel.stopTimer();
                    handleTimeoutOrMiss();
                }
                return;
            }

            int expectedRevealedCount = 7 - (time / 10);
            if (expectedRevealedCount > currentRevealedStep && expectedRevealedCount <= 7) {
                while(currentRevealedStep < expectedRevealedCount) {
                    revealStep(currentRevealedStep);
                    currentRevealedStep++;
                }
            }
            if (time == 0 && currentRevealedStep >= 7 && !isOpponentPhase) {
                sharedViewModel.stopTimer();
                startOpponentPhase();
            }
        });
    }

    private void revealStep(int index) {
        if (index >= 0 && index < steps.size() && index < currentHints.size()) {
            steps.get(index).setText(currentHints.get(index));
        }
    }

    private void checkAnswer() {
        String guess = etStepAnswer.getText() != null ? etStepAnswer.getText().toString().trim() : "";
        if (guess.equalsIgnoreCase(currentAnswer)) {
            sharedViewModel.stopTimer();
            int points = stepByStepViewModel.calculatePoints(currentRevealedStep, isOpponentPhase);
            sharedViewModel.addCurrentPlayerPoints(points);

            if (isOpponentPhase) {
                Toast.makeText(getContext(), "Protivnik je tačno odgovorio! (" + points + " bodova)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Tačno! Osvojeno bodova: " + points, Toast.LENGTH_SHORT).show();
            }
            showAnswerAndAdvance();
        } else {
            Toast.makeText(getContext(), "Netačno!", Toast.LENGTH_SHORT).show();
            etStepAnswer.setText("");
            if (isOpponentPhase) {
                 handleTimeoutOrMiss();
            }
        }
    }


    private void startOpponentPhase() {
        isOpponentPhase = true;
        Toast.makeText(getContext(), "Vreme je isteklo. Protivnik ima 10s za odgovor!", Toast.LENGTH_LONG).show();
        sharedViewModel.startRoundTimer(10, this::handleTimeoutOrMiss);
    }

    private void handleTimeoutOrMiss() {
        showAnswerAndAdvance();
    }

    private void showAnswerAndAdvance() {
        btnConfirmStep.setEnabled(false);
        etStepAnswer.setText(currentAnswer);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                sharedViewModel.advanceGamePhase();
            }
        }, 5000);
    }
}
