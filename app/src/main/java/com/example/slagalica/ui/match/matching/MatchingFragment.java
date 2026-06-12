package com.example.slagalica.ui.match.matching;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.data.GameStateRepository;
import com.example.slagalica.ui.match.MatchViewModel;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class MatchingFragment extends Fragment {

    private MatchViewModel sharedViewModel;
    private GameStateRepository gameStateRepo;
    private ListenerRegistration gameListener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Button[] leftButtons;
    private Button[] rightButtons;
    private TextView tvSpojniceTitle;
    private TextView tvSpojniceTurn;
    private TextView tvFeedback;

    private final String[] leftR1  = {"CECA", "ZDRAVKO COLIC", "DJORDJE BALASEVIC", "LEPA BRENA", "ZELJKO SAMARDZIC"};
    private final String[] rightR1 = {"GRLICA", "RINGISPIL", "TI SI MI U KRVI", "DUGE NOGE", "KUKAVICA"};
    private final int[] correctR1  = {4, 2, 1, 3, 0};

    private final String[] leftR2  = {"FRANCUSKA", "JAPAN", "BRAZIL", "EGIPAT", "AUSTRALIJA"};
    private final String[] rightR2 = {"TOKIO", "BRAZILIJA", "KAIRO", "KANBERA", "PARIZ"};
    private final int[] correctR2  = {4, 0, 1, 2, 3};

    private String[] currentLeft;
    private String[] currentRight;
    private int[] currentCorrect;

    private int selectedLeftIndex = -1;
    private final boolean[] leftMatched  = new boolean[5];
    private final boolean[] rightMatched = new boolean[5];
    private final boolean[] leftFailed   = new boolean[5];
    private int matchedCount = 0;
    private int myScore = 0;

    private int activeFirestorePlayer;
    private boolean isRound1;
    private boolean roundDone = false;
    private boolean opponentPhaseSeen = false;

    private String matchId;
    private String gameKey;

    private static final int COLOR_DEFAULT  = Color.parseColor("#1E3A5F");
    private static final int COLOR_SELECTED = Color.parseColor("#F4A261");
    private static final int COLOR_CORRECT  = Color.parseColor("#E53935");
    private static final int COLOR_FAILED   = Color.parseColor("#374A5E");
    private static final int COLOR_WRONG    = Color.parseColor("#455A7A");
    private static final int COLOR_OPPONENT = Color.parseColor("#508EFA");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_matching, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);
        gameStateRepo = new GameStateRepository();

        matchId = sharedViewModel.getMatchId();
        String phase = sharedViewModel.getCurrentFragment().getValue();
        isRound1 = "SPOJNICE_R1".equals(phase);
        gameKey = isRound1 ? "matching_r1" : "matching_r2";

        activeFirestorePlayer = isRound1 ? 1 : 2;

        tvSpojniceTitle = view.findViewById(R.id.tvSpojniceTitle);
        tvSpojniceTurn  = view.findViewById(R.id.tvSpojniceTurn);
        tvFeedback      = view.findViewById(R.id.tvSpojniceFeedback);

        leftButtons = new Button[]{
                view.findViewById(R.id.btnLeft1), view.findViewById(R.id.btnLeft2),
                view.findViewById(R.id.btnLeft3), view.findViewById(R.id.btnLeft4),
                view.findViewById(R.id.btnLeft5)
        };
        rightButtons = new Button[]{
                view.findViewById(R.id.btnRight1), view.findViewById(R.id.btnRight2),
                view.findViewById(R.id.btnRight3), view.findViewById(R.id.btnRight4),
                view.findViewById(R.id.btnRight5)
        };

        if (isRound1) {
            currentLeft = leftR1; currentRight = rightR1; currentCorrect = correctR1;
            tvSpojniceTitle.setText("SPOJITE PEVACE SA PESMAMA");
        } else {
            currentLeft = leftR2; currentRight = rightR2; currentCorrect = correctR2;
            tvSpojniceTitle.setText("SPOJITE DRZAVE SA GLAVNIM GRADOVIMA");
        }

        for (int i = 0; i < 5; i++) {
            leftButtons[i].setText(currentLeft[i]);
            rightButtons[i].setText(currentRight[i]);
            final int idx = i;
            leftButtons[i].setOnClickListener(v -> onLeftClicked(idx));
            rightButtons[i].setOnClickListener(v -> onRightClicked(idx));
        }

        boolean isInitiator = isRound1 ? sharedViewModel.getIsPlayer1() : !sharedViewModel.getIsPlayer1();
        if (matchId != null && isInitiator) {
            Map<String, Object> data = new HashMap<>();
            data.put("currentPlayer", activeFirestorePlayer);
            data.put("roundDone", false);
            gameStateRepo.set(matchId, gameKey, data);
        }

        listenToGameState();
        updateTurnLabel();


        sharedViewModel.startRoundTimer(30, () -> { if (isMyTurn()) onMyTurnTimedOut(); });
    }


    private void listenToGameState() {
        if (matchId == null) return;
        gameListener = gameStateRepo.listen(matchId, gameKey, (snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;

            Long cp = snapshot.getLong("currentPlayer");
            if (cp != null) activeFirestorePlayer = cp.intValue();

            for (int i = 0; i < 5; i++) {
                Boolean lm = snapshot.getBoolean("leftMatched" + i);
                Long    ri = snapshot.getLong("rightIndex" + i);
                if (Boolean.TRUE.equals(lm) && !leftMatched[i] && ri != null) {
                    int rIdx = ri.intValue();
                    leftMatched[i]  = true;
                    rightMatched[rIdx] = true;
                    leftButtons[i].setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(COLOR_OPPONENT));
                    rightButtons[rIdx].setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(COLOR_OPPONENT));
                    leftButtons[i].setEnabled(false);
                    rightButtons[rIdx].setEnabled(false);
                    matchedCount++;
                }

                Boolean lf = snapshot.getBoolean("leftFailed" + i);
                if (Boolean.TRUE.equals(lf) && !leftFailed[i] && !leftMatched[i]) {
                    leftFailed[i] = true;
                    leftButtons[i].setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(COLOR_FAILED));
                    leftButtons[i].setAlpha(0.5f);
                    leftButtons[i].setEnabled(false);
                }
            }

            Boolean opPhase = snapshot.getBoolean("opponentPhase");
            if (Boolean.TRUE.equals(opPhase) && !opponentPhaseSeen && !roundDone) {
                opponentPhaseSeen = true;
                if (isMyTurn()) {
                    for (int i = 0; i < 5; i++) {
                        if (!leftMatched[i]) {
                            leftFailed[i] = false;
                            leftButtons[i].setAlpha(1.0f);
                            leftButtons[i].setEnabled(true);
                            leftButtons[i].setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(COLOR_DEFAULT));
                        }
                    }
                    showFeedback("Vi igrate preostale pojmove!", true);
                }
                updateTurnLabel();
                sharedViewModel.startRoundTimer(30, () -> { if (isMyTurn()) onMyTurnTimedOut(); });
            }

            updateTurnLabel();

            Boolean done = snapshot.getBoolean("roundDone");
            if (Boolean.TRUE.equals(done) && !roundDone) {
                roundDone = true;
                handler.postDelayed(() -> {
                    if (isAdded()) sharedViewModel.advanceGamePhase();
                }, 1000);
            }
        });
    }

    private boolean isMyTurn() {
        boolean iAmP1 = sharedViewModel.getIsPlayer1();
        return (iAmP1 && activeFirestorePlayer == 1) || (!iAmP1 && activeFirestorePlayer == 2);
    }


    private void onMyTurnTimedOut() {
        if (!isAdded() || roundDone) return;
        if (!isMyTurn()) return;

        for (int i = 0; i < 5; i++) {
            if (!leftMatched[i]) {
                leftFailed[i] = true;
                leftButtons[i].setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(COLOR_FAILED));
                leftButtons[i].setAlpha(0.5f);
                leftButtons[i].setEnabled(false);
            }
        }

        boolean anyRemaining = false;
        for (boolean m : leftMatched) if (!m) { anyRemaining = true; break; }
        if (!anyRemaining) { finishRound(); return; }

        int next = activeFirestorePlayer == 1 ? 2 : 1;
        showFeedback("Protivnik igra preostale pojmove!", false);

        if (matchId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentPlayer", next);
            updates.put("opponentPhase", true);
            for (int i = 0; i < 5; i++) updates.put("leftFailed" + i, leftFailed[i]);
            gameStateRepo.update(matchId, gameKey, updates);
        }
        activeFirestorePlayer = next;
        updateTurnLabel();
    }


    private void onLeftClicked(int index) {
        if (!isMyTurn() || leftMatched[index] || leftFailed[index] || roundDone) return;

        if (selectedLeftIndex >= 0 && selectedLeftIndex != index) {
            leftButtons[selectedLeftIndex].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            leftFailed[selectedLeftIndex] ? COLOR_FAILED : COLOR_DEFAULT));
        }
        if (selectedLeftIndex == index) {
            leftButtons[index].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(COLOR_DEFAULT));
            selectedLeftIndex = -1;
            return;
        }
        selectedLeftIndex = index;
        leftButtons[index].setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(COLOR_SELECTED));
    }

    private void onRightClicked(int rightIndex) {
        if (!isMyTurn() || rightMatched[rightIndex] || selectedLeftIndex < 0 || roundDone) return;

        boolean isCorrect = currentCorrect[selectedLeftIndex] == rightIndex;
        int prevLeft = selectedLeftIndex;
        selectedLeftIndex = -1;

        if (isCorrect) {
            leftButtons[prevLeft].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(COLOR_CORRECT));
            rightButtons[rightIndex].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(COLOR_CORRECT));
            leftButtons[prevLeft].setEnabled(false);
            rightButtons[rightIndex].setEnabled(false);
            leftMatched[prevLeft] = true;
            rightMatched[rightIndex] = true;
            leftFailed[prevLeft] = false;
            myScore += 2;
            matchedCount++;
            showFeedback("+2 boda!", true);

            if (matchId != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("leftMatched" + prevLeft, true);
                updates.put("rightIndex" + prevLeft, rightIndex);
                gameStateRepo.update(matchId, gameKey, updates);
            }

            if (matchedCount == 5) {
                sharedViewModel.stopTimer();
                finishRound();
            }
        } else {
            leftFailed[prevLeft] = true;
            leftButtons[prevLeft].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(COLOR_FAILED));
            leftButtons[prevLeft].setAlpha(0.5f);
            leftButtons[prevLeft].setEnabled(false);
            rightButtons[rightIndex].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(COLOR_WRONG));
            showFeedback("Nije tacno!", false);

            handler.postDelayed(() -> {
                if (!isAdded()) return;
                rightButtons[rightIndex].setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(COLOR_DEFAULT));
                if (matchedCount + countFailed() == 5) {
                    sharedViewModel.stopTimer();
                    handler.postDelayed(this::onMyTurnTimedOut, 1500);
                }
            }, 700);
        }
    }


    private void finishRound() {
        if (roundDone) return;
        roundDone = true;
        sharedViewModel.stopTimer();
        sharedViewModel.addCurrentPlayerPoints(myScore);

        if (matchId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("roundDone", true);
            updates.put("totalScore", myScore);
            gameStateRepo.update(matchId, gameKey, updates);
        }
        handler.postDelayed(() -> {
            if (isAdded()) sharedViewModel.advanceGamePhase();
        }, 1000);
    }


    private void updateTurnLabel() {
        String runda = isRound1 ? "Runda 1" : "Runda 2";
        tvSpojniceTurn.setText(runda + " — " + (isMyTurn() ? "Vi igrate" : "Protivnik igra"));
    }

    private int countFailed() {
        int count = 0;
        for (boolean f : leftFailed) if (f) count++;
        return count;
    }

    private void showFeedback(String msg, boolean positive) {
        tvFeedback.setText(msg);
        tvFeedback.setTextColor(positive ? Color.parseColor("#2EC27E") : Color.parseColor("#F4A261"));
        tvFeedback.setVisibility(View.VISIBLE);
        handler.postDelayed(() -> {
            if (isAdded()) tvFeedback.setVisibility(View.INVISIBLE);
        }, 1500);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (gameListener != null) gameListener.remove();
    }
}
