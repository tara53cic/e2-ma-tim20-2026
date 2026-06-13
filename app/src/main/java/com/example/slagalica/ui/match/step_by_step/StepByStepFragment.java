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
import com.example.slagalica.data.GameStateRepository;
import com.example.slagalica.data.UserStatsRepository;
import com.example.slagalica.domain.models.StepByStep;
import com.example.slagalica.ui.match.MatchViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StepByStepFragment extends Fragment {

    private MatchViewModel sharedViewModel;
    private StepByStepViewModel stepByStepViewModel;
    private GameStateRepository gameStateRepo;
    private final UserStatsRepository statsRepo = new UserStatsRepository();
    private ListenerRegistration gameListener;

    private List<TextView> steps = new ArrayList<>();
    private TextInputEditText etStepAnswer;
    private MaterialButton btnConfirmStep;
    private TextView tvTurnIndicator;

    private List<String> currentHints = new ArrayList<>();
    private String currentAnswer = "";

    private int currentRevealedStep = 0;
    private boolean isOpponentPhase = false;
    private boolean roundDone = false;
    private boolean statsWritten = false;

    private String matchId;
    private String gameKey;
    private boolean isActivePlayer;

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
        gameStateRepo = new GameStateRepository();

        matchId = sharedViewModel.getMatchId();
        String phase = sharedViewModel.getCurrentFragment().getValue();
        boolean isR1 = "KORAK_PO_KORAK_R1".equals(phase);
        gameKey = isR1 ? "step_r1" : "step_r2";
        isActivePlayer = isR1 ? sharedViewModel.getIsPlayer1() : !sharedViewModel.getIsPlayer1();

        ViewGroup stepsContainer = view.findViewById(R.id.stepsContainer);
        if (stepsContainer != null) {
            for (int i = 0; i < stepsContainer.getChildCount(); i++) {
                View child = stepsContainer.getChildAt(i);
                if (child instanceof TextView) steps.add((TextView) child);
            }
        }

        etStepAnswer = view.findViewById(R.id.etStepAnswer);
        btnConfirmStep = view.findViewById(R.id.btnConfirmStep);
        tvTurnIndicator = view.findViewById(R.id.tvTurnIndicator);

        etStepAnswer.setEnabled(isActivePlayer);
        btnConfirmStep.setEnabled(isActivePlayer);

        stepByStepViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && !loading) {
                List<StepByStep> games = stepByStepViewModel.getGames().getValue();
                if (games != null && !games.isEmpty()) {
                    StepByStep currentData = isR1 ? games.get(0) : games.get(games.size() > 1 ? 1 : 0);
                    currentHints.clear();
                    for (int i = 1; i <= 7; i++) currentHints.add(currentData.getStep(i));
                    currentAnswer = currentData.getFinal_answer();
                    setupRound();
                } else {
                    Toast.makeText(getContext(), getString(R.string.error_loading_data), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupRound() {
        if (isActivePlayer) {
            btnConfirmStep.setOnClickListener(v -> checkAnswer());
        }

        currentRevealedStep = 0;
        isOpponentPhase = false;

        if (tvTurnIndicator != null) {
            tvTurnIndicator.setText(isActivePlayer ? "Vi ste na potezu" : "Protivnik je na potezu");
        }

        listenToGameState();

        sharedViewModel.getTimeRemaining().observe(getViewLifecycleOwner(), time -> {
            if (roundDone) return;
            if (isOpponentPhase) {
                if (time != null && time == 0) {
                    sharedViewModel.stopTimer();
                    if (isActivePlayer) publishOutcome(false, 0);
                }
                return;
            }

            int expectedRevealedCount = 7 - (time / 10);
            if (expectedRevealedCount > currentRevealedStep && expectedRevealedCount <= 7) {
                while (currentRevealedStep < expectedRevealedCount) {
                    revealStep(currentRevealedStep);
                    currentRevealedStep++;
                }
            }
            if (time != null && time == 0 && currentRevealedStep >= 7) {
                sharedViewModel.stopTimer();
                if (isActivePlayer) {
                    startOpponentPhase();
                }
            }
        });
    }

    private void revealStep(int index) {
        if (index >= 0 && index < steps.size() && index < currentHints.size()) {
            steps.get(index).setText(currentHints.get(index));
        }
    }

    private void checkAnswer() {
        if (roundDone) return;
        String guess = etStepAnswer.getText() != null ? etStepAnswer.getText().toString().trim() : "";
        if (guess.equalsIgnoreCase(currentAnswer)) {
            sharedViewModel.stopTimer();
            int points = stepByStepViewModel.calculatePoints(currentRevealedStep, isOpponentPhase);
            publishOutcome(true, points);
        } else {
            Toast.makeText(getContext(), getString(R.string.incorrect_answer), Toast.LENGTH_SHORT).show();
            etStepAnswer.setText("");
            if (isOpponentPhase) {
                publishOutcome(false, 0);
            }
        }
    }

    private void startOpponentPhase() {
        isOpponentPhase = true;
        etStepAnswer.setEnabled(false);
        btnConfirmStep.setEnabled(false);
        if (tvTurnIndicator != null) {
            tvTurnIndicator.setText("Protivnik je na potezu");
        }
        sharedViewModel.startRoundTimer(10, () -> {
            if (!roundDone) publishOutcome(false, 0);
        });
        if (matchId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("phase", "OPPONENT");
            gameStateRepo.update(matchId, gameKey, updates);
        }
    }

    private void publishOutcome(boolean correct, int points) {
        if (roundDone) return;
        roundDone = true;
        sharedViewModel.stopTimer();

        if (isActivePlayer && !isOpponentPhase) {
            writeStats(correct, currentRevealedStep, points);
        } else if (!isActivePlayer && isOpponentPhase) {
        }

        if (correct) sharedViewModel.addCurrentPlayerPoints(points);
        if (matchId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("phase", "DONE");
            updates.put("correct", correct);
            updates.put("points", points);
            updates.put("answer", currentAnswer);
            gameStateRepo.update(matchId, gameKey, updates);
        }
        showAnswerAndAdvance(correct, points);
    }

    private void writeStats(boolean correct, int revealedStepCount, int points) {
        if (statsWritten) return;
        statsWritten = true;
        String uid = statsRepo.getCurrentUid();
        if (uid != null) {
            int solvedAtStep = correct ? revealedStepCount : -1;
            statsRepo.recordKpk(uid, solvedAtStep, points);
        }
    }

    private void listenToGameState() {
        if (matchId == null) return;
        if (isActivePlayer) {
            Map<String, Object> init = new HashMap<>();
            init.put("phase", "PLAYER");
            gameStateRepo.set(matchId, gameKey, init);
        }

        gameListener = gameStateRepo.listen(matchId, gameKey, (snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;
            String phase = snapshot.getString("phase");
            if (roundDone) return;

            if ("OPPONENT".equals(phase) && !isActivePlayer && !isOpponentPhase) {
                isOpponentPhase = true;
                etStepAnswer.setEnabled(true);
                btnConfirmStep.setEnabled(true);
                btnConfirmStep.setOnClickListener(v -> checkAnswer());
                if (tvTurnIndicator != null) {
                    tvTurnIndicator.setText("Vi ste na potezu");
                }
                sharedViewModel.startRoundTimer(10, () -> {
                    if (!roundDone && isAdded()) {
                        etStepAnswer.setEnabled(false);
                        btnConfirmStep.setEnabled(false);
                    }
                });
            }

            if ("DONE".equals(phase)) {
                if (roundDone) return;
                roundDone = true;
                sharedViewModel.stopTimer();
                Boolean correct = snapshot.getBoolean("correct");
                Long pts = snapshot.getLong("points");
                String answer = snapshot.getString("answer");
                int points = pts != null ? pts.intValue() : 0;
                if (answer != null) currentAnswer = answer;
                showAnswerAndAdvance(Boolean.TRUE.equals(correct), points);
            }
        });
    }

    private void showAnswerAndAdvance(boolean correct, int points) {
        btnConfirmStep.setEnabled(false);
        etStepAnswer.setText(currentAnswer);
        if (correct && isActivePlayer) {
            Toast.makeText(getContext(),
                    isOpponentPhase
                            ? getString(R.string.opponent_correct_answer, points)
                            : getString(R.string.correct_answer_points, points),
                    Toast.LENGTH_SHORT).show();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) sharedViewModel.advanceGamePhase();
        }, 5000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (gameListener != null) gameListener.remove();
    }
}