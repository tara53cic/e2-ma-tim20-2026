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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class WhoKnowsFragment extends Fragment {

    private MatchViewModel sharedViewModel;

    private TextView tvQuestionNumber, tvQuestion, tvFeedback;
    private MaterialButton btnAnswerA, btnAnswerB, btnAnswerC, btnAnswerD;

    private int currentQuestion = 0;
    private final int TOTAL_QUESTIONS = 5;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final boolean[] questionAdvanced = new boolean[5];
    private boolean gameAdvanceCalled = false;

    private String matchId;
    private ListenerRegistration questionListener;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_who_knows, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);
        matchId = sharedViewModel.getMatchId();

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

        startListeningForQuestions();
        loadQuestion(0);
    }

    private void startListeningForQuestions() {
        if (matchId == null) return;
        questionListener = FirebaseFirestore.getInstance()
                .collection("matches").document(matchId)
                .collection("games").document("kzz")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    for (int i = 0; i < TOTAL_QUESTIONS; i++) {
                        Boolean done = snapshot.getBoolean("q" + i + "_done");
                        if (Boolean.TRUE.equals(done) && i == currentQuestion && !questionAdvanced[i]) {
                            advanceQuestion(i); // other phone answered before us
                        }
                    }

                    Boolean p1done = snapshot.getBoolean("p1done");
                    Boolean p2done = snapshot.getBoolean("p2done");
                    if (Boolean.TRUE.equals(p1done) && Boolean.TRUE.equals(p2done)) {
                        advanceGamePhase();
                    }
                });
    }


    private void loadQuestion(final int index) {
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
            if (questionAdvanced[index]) return;

            setButtonsEnabled(false);
            tvFeedback.setText("Vreme isteklo!");
            tvFeedback.setTextColor(android.graphics.Color.parseColor("#F4A261"));
            tvFeedback.setVisibility(View.VISIBLE);

            // Mark as done in Firestore so the other phone can also move on
            writeQuestionDone(index);
            advanceQuestion(index);
        });
    }


    private void onAnswerClicked(int selectedIndex) {
        if (questionAdvanced[currentQuestion]) return;

        setButtonsEnabled(false);
        boolean isCorrect = selectedIndex == correctAnswers[currentQuestion];
        MaterialButton[] buttons = {btnAnswerA, btnAnswerB, btnAnswerC, btnAnswerD};

        if (isCorrect) {
            buttons[selectedIndex].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2EC27E")));
            tvFeedback.setText("Tacno! +10 bodova");
            tvFeedback.setTextColor(android.graphics.Color.parseColor("#2EC27E"));
            sharedViewModel.addCurrentPlayerPoints(10);
        } else {
            buttons[selectedIndex].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E53935")));
            buttons[correctAnswers[currentQuestion]].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2EC27E")));
            tvFeedback.setText("Netacno! -5 bodova");
            tvFeedback.setTextColor(android.graphics.Color.parseColor("#E53935"));
            sharedViewModel.addCurrentPlayerPoints(-5);
        }
        tvFeedback.setVisibility(View.VISIBLE);

        writeQuestionDone(currentQuestion);
        advanceQuestion(currentQuestion);
    }


    private void writeQuestionDone(int qIndex) {
        if (matchId == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("q" + qIndex + "_done", true);
        FirebaseFirestore.getInstance()
                .collection("matches").document(matchId)
                .collection("games").document("kzz")
                .set(data, SetOptions.merge());
    }

    private void advanceQuestion(int qIndex) {
        if (questionAdvanced[qIndex]) return;
        questionAdvanced[qIndex] = true;
        sharedViewModel.stopTimer();

        handler.postDelayed(() -> {
            if (!isAdded()) return;
            currentQuestion = qIndex + 1;
            if (currentQuestion < TOTAL_QUESTIONS) {
                loadQuestion(currentQuestion);
            } else {
                markAllQuestionsAnswered();
            }
        }, 1500);
    }

    private void markAllQuestionsAnswered() {
        if (matchId == null) { advanceGamePhase(); return; }
        String field = sharedViewModel.getIsPlayer1() ? "p1done" : "p2done";
        Map<String, Object> data = new HashMap<>();
        data.put(field, true);
        FirebaseFirestore.getInstance()
                .collection("matches").document(matchId)
                .collection("games").document("kzz")
                .set(data, SetOptions.merge());

    }

    private void advanceGamePhase() {
        if (gameAdvanceCalled || !isAdded()) return;
        gameAdvanceCalled = true;
        sharedViewModel.stopTimer();
        sharedViewModel.advanceGamePhase();
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
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(surfaceColor));
        }
        setButtonsEnabled(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (questionListener != null) questionListener.remove();
    }
}
