package com.example.slagalica.ui.match.who_knows;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.ui.match.MatchViewModel;
import com.google.android.material.button.MaterialButton;

public class WhoKnowsFragment extends Fragment {

    private MatchViewModel sharedViewModel;

    private TextView tvQuestionNumber;
    private TextView tvQuestion;
    private TextView tvFeedback;
    private MaterialButton btnAnswerA;
    private MaterialButton btnAnswerB;
    private MaterialButton btnAnswerC;
    private MaterialButton btnAnswerD;

    private int currentQuestion = 0;
    private final int TOTAL_QUESTIONS = 5;
    private final Handler handler = new Handler(Looper.getMainLooper());

    //Placeholder za pitanja
    private final String[] questions = {
            "Koji grad je glavni grad Srbije?",
            "Koja je najduza reka na svetu?",
            "Koliko igraca ima fudbalski tim?",
            "Koji element ima hemijski simbol O?",
            "U kojoj zemlji se nalazi Ajfelov toranj?"
    };

    private final String[][] answers = {
            {"A) Beograd", "B) Novi Sad", "C) Nis", "D) Kragujevac"},
            {"A) Amazon", "B) Nil", "C) Dunav", "D) Mississippi"},
            {"A) 10", "B) 9", "C) 11", "D) 12"},
            {"A) Zlato", "B) Kiseonik", "C) Vodonik", "D) Azot"},
            {"A) Spanija", "B) Italija", "C) Francuska", "D) Nemacka"}
    };

    private final int[] correctAnswers = {0, 1, 2, 1, 2};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_who_knows, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);

        tvQuestionNumber = view.findViewById(R.id.tvQuestionNumber);
        tvQuestion       = view.findViewById(R.id.tvQuestion);
        tvFeedback       = view.findViewById(R.id.tvFeedback);
        btnAnswerA       = view.findViewById(R.id.btnAnswerA);
        btnAnswerB       = view.findViewById(R.id.btnAnswerB);
        btnAnswerC       = view.findViewById(R.id.btnAnswerC);
        btnAnswerD       = view.findViewById(R.id.btnAnswerD);

        btnAnswerA.setOnClickListener(v -> onAnswerClicked(0));
        btnAnswerB.setOnClickListener(v -> onAnswerClicked(1));
        btnAnswerC.setOnClickListener(v -> onAnswerClicked(2));
        btnAnswerD.setOnClickListener(v -> onAnswerClicked(3));

        loadQuestion(currentQuestion);
    }

    private void loadQuestion(int index) {
        tvQuestionNumber.setText("Pitanje " + (index + 1) + "/" + TOTAL_QUESTIONS);
        tvQuestion.setText(questions[index]);
        btnAnswerA.setText(answers[index][0]);
        btnAnswerB.setText(answers[index][1]);
        btnAnswerC.setText(answers[index][2]);
        btnAnswerD.setText(answers[index][3]);
        resetButtons();
        tvFeedback.setVisibility(View.INVISIBLE);

        sharedViewModel.startRoundTimer(5, () -> {
            if (!isAdded()) return;
            // Isteklo vreme
            setButtonsEnabled(false);
            tvFeedback.setText("Vreme isteklo!");
            tvFeedback.setTextColor(android.graphics.Color.parseColor("#F4A261"));
            tvFeedback.setVisibility(View.VISIBLE);
            handler.postDelayed(() -> {
                if (!isAdded()) return;
                currentQuestion++;
                if (currentQuestion < TOTAL_QUESTIONS) {
                    loadQuestion(currentQuestion);
                } else {
                    sharedViewModel.advanceGamePhase();
                }
            }, 1500);
        });
    }

    private void onAnswerClicked(int selectedIndex) {
        setButtonsEnabled(false);

        boolean isCorrect = selectedIndex == correctAnswers[currentQuestion];
        MaterialButton[] buttons = {btnAnswerA, btnAnswerB, btnAnswerC, btnAnswerD};

        if (isCorrect) {
            buttons[selectedIndex].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#2EC27E")));
            tvFeedback.setText("Tacno! +10 bodova");
            tvFeedback.setTextColor(android.graphics.Color.parseColor("#2EC27E"));
            sharedViewModel.addCurrentPlayerPoints(10);
        } else {
            buttons[selectedIndex].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#E53935")));

            buttons[correctAnswers[currentQuestion]].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#2EC27E")));
            tvFeedback.setText("Netacno! -5 bodova");
            tvFeedback.setTextColor(android.graphics.Color.parseColor("#E53935"));
            sharedViewModel.addCurrentPlayerPoints(-5);
        }

        tvFeedback.setVisibility(View.VISIBLE);

        handler.postDelayed(() -> {
            if (!isAdded()) return;
            currentQuestion++;
            if (currentQuestion < TOTAL_QUESTIONS) {
                loadQuestion(currentQuestion);
            } else {
                sharedViewModel.advanceGamePhase();
            }
        }, 2000);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnAnswerA.setEnabled(enabled);
        btnAnswerB.setEnabled(enabled);
        btnAnswerC.setEnabled(enabled);
        btnAnswerD.setEnabled(enabled);
    }

    private void resetButtons() {
        int surfaceColor = android.graphics.Color.parseColor("#1E3A5F");
        MaterialButton[] buttons = {btnAnswerA, btnAnswerB, btnAnswerC, btnAnswerD};
        for (MaterialButton btn : buttons) {
            btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(surfaceColor));
        }
        setButtonsEnabled(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}