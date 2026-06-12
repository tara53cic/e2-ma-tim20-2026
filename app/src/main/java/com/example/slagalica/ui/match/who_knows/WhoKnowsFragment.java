package com.example.slagalica.ui.match.who_knows;

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
import com.example.slagalica.domain.models.WhoKnowsQuestion;
import com.example.slagalica.ui.match.MatchViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhoKnowsFragment extends Fragment {

    private WhoKnowsViewModel viewModel;
    private MatchViewModel sharedViewModel;

    private TextView tvQuestionNumber, tvQuestion, tvFeedback;
    private MaterialButton btnAnswerA, btnAnswerB, btnAnswerC, btnAnswerD;

    private int currentQuestion = 0;
    private static final int TOTAL_QUESTIONS = 5;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final boolean[] questionResolved = new boolean[TOTAL_QUESTIONS];
    private boolean gameAdvanceCalled = false;
    private boolean dataLoaded = false;
    private boolean answeredCurrent = false;

    private String matchId;
    private ListenerRegistration questionListener;
    private ListenerRegistration dataListener;

    private final List<WhoKnowsQuestion> questions = new ArrayList<>();
    private static final String[] PREFIXES = {"A) ", "B) ", "C) ", "D) "};

    private static final int COLOR_DEFAULT  = android.graphics.Color.parseColor("#1E3A5F");
    private static final int COLOR_CORRECT  = android.graphics.Color.parseColor("#2EC27E");
    private static final int COLOR_WRONG    = android.graphics.Color.parseColor("#E53935");
    private static final int COLOR_NEUTRAL  = android.graphics.Color.parseColor("#607D8B");

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_who_knows, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel       = new ViewModelProvider(this).get(WhoKnowsViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);
        matchId = sharedViewModel.getMatchId();

        tvQuestionNumber = view.findViewById(R.id.tvQuestionNumber);
        tvQuestion       = view.findViewById(R.id.tvQuestion);
        tvFeedback       = view.findViewById(R.id.tvFeedback);
        btnAnswerA       = view.findViewById(R.id.btnAnswerA);
        btnAnswerB       = view.findViewById(R.id.btnAnswerB);
        btnAnswerC       = view.findViewById(R.id.btnAnswerC);
        btnAnswerD       = view.findViewById(R.id.btnAnswerD);

        setButtonsEnabled(false);
        tvQuestion.setText("Učitavanje pitanja...");

        if (sharedViewModel.getIsPlayer1()) {
            viewModel.loadAndPublish(matchId);
            viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                if (loading == null || loading) return;
                onDataReady();
            });
        } else {
            waitForData();
        }
    }

    private void waitForData() {
        if (matchId == null) return;
        dataListener = FirebaseFirestore.getInstance()
                .collection("matches").document(matchId)
                .collection("games").document("kzz_data")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists() || dataLoaded) return;
                    viewModel.loadFromMatch(matchId);
                    viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                        if (loading == null || loading) return;
                        onDataReady();
                    });
                });
    }

    private void onDataReady() {
        if (dataLoaded) return;
        List<WhoKnowsQuestion> loaded = viewModel.getQuestions().getValue();
        if (loaded == null || loaded.size() < TOTAL_QUESTIONS) {
            tvQuestion.setText("Greška: nema dovoljno pitanja u bazi.");
            Toast.makeText(getContext(), "Greška pri učitavanju pitanja!", Toast.LENGTH_LONG).show();
            return;
        }
        questions.clear();
        questions.addAll(loaded);
        dataLoaded = true;

        btnAnswerA.setOnClickListener(v -> onAnswerClicked(0));
        btnAnswerB.setOnClickListener(v -> onAnswerClicked(1));
        btnAnswerC.setOnClickListener(v -> onAnswerClicked(2));
        btnAnswerD.setOnClickListener(v -> onAnswerClicked(3));

        startListening();
        loadQuestion(0);
    }

    private void loadQuestion(final int index) {
        if (index >= questions.size()) return;
        answeredCurrent = false;

        WhoKnowsQuestion q = questions.get(index);
        List<String> ans = q.getAnswers();

        tvQuestionNumber.setText("Pitanje " + (index + 1) + "/" + TOTAL_QUESTIONS);
        tvQuestion.setText(q.getQuestion());

        MaterialButton[] buttons = {btnAnswerA, btnAnswerB, btnAnswerC, btnAnswerD};
        if (ans != null && ans.size() >= 4) {
            for (int i = 0; i < 4; i++) buttons[i].setText(PREFIXES[i] + ans.get(i));
        }
        resetButtons();
        tvFeedback.setVisibility(View.INVISIBLE);

        sharedViewModel.startRoundTimer(5, () -> {
            if (!isAdded() || questionResolved[index]) return;
            // Ako nisam odgovorio — upiši -1 (nije odgovorio)
            if (!answeredCurrent) {
                writeAnswer(index, -1, System.currentTimeMillis());
            }
        });
    }

    private void onAnswerClicked(int selectedIndex) {
        if (answeredCurrent || questionResolved[currentQuestion]) return;
        answeredCurrent = true;
        setButtonsEnabled(false);

        writeAnswer(currentQuestion, selectedIndex, System.currentTimeMillis());

        MaterialButton[] buttons = {btnAnswerA, btnAnswerB, btnAnswerC, btnAnswerD};
        setButtonColor(buttons[selectedIndex], COLOR_NEUTRAL);
        tvFeedback.setText("Čekanje protivnika...");
        tvFeedback.setTextColor(android.graphics.Color.WHITE);
        tvFeedback.setVisibility(View.VISIBLE);
    }

    private void writeAnswer(int qIndex, int answerIndex, long timestamp) {
        if (matchId == null) return;
        String prefix = sharedViewModel.getIsPlayer1() ? "p1" : "p2";
        Map<String, Object> data = new HashMap<>();
        data.put(prefix + "_ans_" + qIndex, answerIndex);   // -1 = nije odgovorio
        data.put(prefix + "_ts_" + qIndex, timestamp);
        FirebaseFirestore.getInstance()
                .collection("matches").document(matchId)
                .collection("games").document("kzz")
                .set(data, SetOptions.merge());
    }

    private void startListening() {
        if (matchId == null) return;
        questionListener = FirebaseFirestore.getInstance()
                .collection("matches").document(matchId)
                .collection("games").document("kzz")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    for (int i = 0; i < TOTAL_QUESTIONS; i++) {
                        if (questionResolved[i]) continue;

                        Long p1ans = snapshot.getLong("p1_ans_" + i);
                        Long p2ans = snapshot.getLong("p2_ans_" + i);

                        if (p1ans != null && p2ans != null && i == currentQuestion) {
                            Long p1ts = snapshot.getLong("p1_ts_" + i);
                            Long p2ts = snapshot.getLong("p2_ts_" + i);
                            evaluateQuestion(i, p1ans.intValue(), p2ans.intValue(),
                                    p1ts != null ? p1ts : Long.MAX_VALUE,
                                    p2ts != null ? p2ts : Long.MAX_VALUE);
                        }
                    }

                    Boolean p1done = snapshot.getBoolean("p1done");
                    Boolean p2done = snapshot.getBoolean("p2done");
                    if (Boolean.TRUE.equals(p1done) && Boolean.TRUE.equals(p2done)) {
                        advanceGamePhase();
                    }
                });
    }

    private void evaluateQuestion(int qIndex, int p1ans, int p2ans, long p1ts, long p2ts) {
        if (questionResolved[qIndex]) return;
        questionResolved[qIndex] = true;
        sharedViewModel.stopTimer();

        int correct = questions.get(qIndex).getCorrectIndex();
        boolean p1correct = p1ans == correct;
        boolean p2correct = p2ans == correct;
        boolean iAmP1 = sharedViewModel.getIsPlayer1();

        int myPoints = 0;
        String feedbackMsg;
        int feedbackColor;

        MaterialButton[] buttons = {btnAnswerA, btnAnswerB, btnAnswerC, btnAnswerD};
        setButtonColor(buttons[correct], COLOR_CORRECT);

        if (p1correct && p2correct) {
            boolean p1faster = p1ts <= p2ts;
            boolean iWon = iAmP1 ? p1faster : !p1faster;
            if (iWon) {
                myPoints = 10;
                feedbackMsg = "Tačno! Brži si! +10";
                feedbackColor = COLOR_CORRECT;
            } else {
                myPoints = 0;
                feedbackMsg = "Tačno, ali protivnik je brži! +0";
                feedbackColor = COLOR_NEUTRAL;
            }
        } else if (iAmP1 && p1correct) {
            myPoints = 10;
            feedbackMsg = "Tačno! +10";
            feedbackColor = COLOR_CORRECT;
        } else if (!iAmP1 && p2correct) {
            myPoints = 10;
            feedbackMsg = "Tačno! +10";
            feedbackColor = COLOR_CORRECT;
        } else if (iAmP1 && p1ans != -1 && !p1correct) {
            myPoints = -5;
            feedbackMsg = "Netačno! -5";
            feedbackColor = COLOR_WRONG;
            if (p1ans >= 0 && p1ans < 4) setButtonColor(buttons[p1ans], COLOR_WRONG);
        } else if (!iAmP1 && p2ans != -1 && !p2correct) {
            myPoints = -5;
            feedbackMsg = "Netačno! -5";
            feedbackColor = COLOR_WRONG;
            if (p2ans >= 0 && p2ans < 4) setButtonColor(buttons[p2ans], COLOR_WRONG);
        } else {
            if ((!iAmP1 && p1correct) || (iAmP1 && p2correct)) {
                myPoints = 0;
                feedbackMsg = "Protivnik tačno odgovorio! +0";
                feedbackColor = COLOR_NEUTRAL;
            } else {
                myPoints = 0;
                feedbackMsg = "Niko nije odgovorio!";
                feedbackColor = COLOR_NEUTRAL;
            }
        }

        if (myPoints != 0) sharedViewModel.addCurrentPlayerPoints(myPoints);

        tvFeedback.setText(feedbackMsg);
        tvFeedback.setTextColor(feedbackColor);
        tvFeedback.setVisibility(View.VISIBLE);

        handler.postDelayed(() -> {
            if (!isAdded()) return;
            currentQuestion = qIndex + 1;
            if (currentQuestion < TOTAL_QUESTIONS) {
                loadQuestion(currentQuestion);
            } else {
                markDone();
            }
        }, 1500);
    }

    private void markDone() {
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

    private void setButtonColor(MaterialButton btn, int color) {
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void resetButtons() {
        for (MaterialButton btn : new MaterialButton[]{btnAnswerA, btnAnswerB, btnAnswerC, btnAnswerD}) {
            setButtonColor(btn, COLOR_DEFAULT);
        }
        setButtonsEnabled(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (questionListener != null) questionListener.remove();
        if (dataListener != null) dataListener.remove();
    }
}