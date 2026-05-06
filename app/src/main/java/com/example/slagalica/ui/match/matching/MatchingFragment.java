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
import com.example.slagalica.ui.match.MatchViewModel;

public class MatchingFragment extends Fragment {

    private MatchViewModel sharedViewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Button[] leftButtons;
    private Button[] rightButtons;
    private TextView tvSpojniceTitle;
    private TextView tvSpojniceTurn;
    private TextView tvFeedback;

    // Runda 1: Pjevaci i pjesme
    private final String[] leftR1  = {"CECA", "ZDRAVKO COLIC", "DJORDJE BALASEVIC", "LEPA BRENA", "ZELJKO SAMARDZIC"};

    private final String[] rightR1 = {"GRLICA", "RINGISPIL", "TI SI MI U KRVI", "DUGE NOGE", "KUKAVICA"};

    private final int[] correctR1 = {4, 2, 1, 3, 0};

    // Runda 2: Drzave i glavni gradovi
    private final String[] leftR2  = {"FRANCUSKA", "JAPAN", "BRAZIL", "EGIPAT", "AUSTRALIJA"};
    private final String[] rightR2 = {"TOKIO", "BRAZILIJA", "KAIRO", "KANBERA", "PARIZ"};

    private final int[] correctR2 = {4, 0, 1, 2, 3};

    private String[] currentLeft;
    private String[] currentRight;
    private int[] currentCorrect;

    private int selectedLeftIndex = -1;
    private final boolean[] leftMatched   = new boolean[5];
    private final boolean[] rightMatched  = new boolean[5];
    private final boolean[] leftFailed    = new boolean[5]; // zasivljeni
    private int matchedCount = 0;
    private int myScore = 0;

    private boolean isOpponentPhase = false;
    private boolean isRound1;


    private static final int COLOR_DEFAULT  = Color.parseColor("#1E3A5F");
    private static final int COLOR_SELECTED = Color.parseColor("#F4A261");
    private static final int COLOR_CORRECT  = Color.parseColor("#E53935");
    private static final int COLOR_FAILED   = Color.parseColor("#374A5E");
    private static final int COLOR_WRONG    = Color.parseColor("#455A7A");
    private static final int COLOR_OPPONENT = Color.parseColor("#508EFA");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_matching, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);

        tvSpojniceTitle = view.findViewById(R.id.tvSpojniceTitle);
        tvSpojniceTurn  = view.findViewById(R.id.tvSpojniceTurn);
        tvFeedback      = view.findViewById(R.id.tvSpojniceFeedback);

        leftButtons = new Button[]{
                view.findViewById(R.id.btnLeft1),
                view.findViewById(R.id.btnLeft2),
                view.findViewById(R.id.btnLeft3),
                view.findViewById(R.id.btnLeft4),
                view.findViewById(R.id.btnLeft5)
        };

        rightButtons = new Button[]{
                view.findViewById(R.id.btnRight1),
                view.findViewById(R.id.btnRight2),
                view.findViewById(R.id.btnRight3),
                view.findViewById(R.id.btnRight4),
                view.findViewById(R.id.btnRight5)
        };

        String phase = sharedViewModel.getCurrentFragment().getValue();
        isRound1 = "SPOJNICE_R1".equals(phase);

        if (isRound1) {
            currentLeft    = leftR1;
            currentRight   = rightR1;
            currentCorrect = correctR1;
            tvSpojniceTitle.setText("SPOJITE PEVACE SA PESMAMA");
        } else {
            currentLeft    = leftR2;
            currentRight   = rightR2;
            currentCorrect = correctR2;
            tvSpojniceTitle.setText("SPOJITE DRZAVE SA GLAVNIM GRADOVIMA");
        }

        for (int i = 0; i < 5; i++) {
            leftButtons[i].setText(currentLeft[i]);
            rightButtons[i].setText(currentRight[i]);
            final int index = i;
            leftButtons[i].setOnClickListener(v -> onLeftClicked(index));
            rightButtons[i].setOnClickListener(v -> onRightClicked(index));
        }

        if (isRound1) {
            startPlayerPhase();
        } else {
            startOpponentPhase();
        }
    }

    private void startPlayerPhase() {
        isOpponentPhase = false;
        updateTurnLabel();
        sharedViewModel.startRoundTimer(30, this::onPlayerTimeUp);
    }

    private void onPlayerTimeUp() {
        if (!isAdded()) return;

        for (int i = 0; i < 5; i++) {
            if (!leftMatched[i]) {
                leftFailed[i] = true;
                leftButtons[i].setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(COLOR_FAILED));
                leftButtons[i].setAlpha(0.5f);
            }
        }

        boolean hasRemaining = false;
        for (boolean matched : leftMatched) {
            if (!matched) { hasRemaining = true; break; }
        }

        if (!hasRemaining) {
            finishRound();
            return;
        }

        startOpponentPhase();
    }

    private void startOpponentPhase() {
        isOpponentPhase = true;
        updateTurnLabel();
        showFeedback("Protivnik igra preostale pojmove!", false);

        for (int i = 0; i < 5; i++) {
            if (!leftMatched[i]) {
                leftFailed[i] = false;
                leftButtons[i].setAlpha(1.0f);
                leftButtons[i].setEnabled(true);
                leftButtons[i].setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(COLOR_DEFAULT));
            }
        }

        sharedViewModel.startRoundTimer(30, () -> {
            if (!isAdded()) return;
            if (!isRound1) {
                onOpponentTimeUp();
            } else {
                finishRound();
            }
        });
    }

    private void onOpponentTimeUp() {
        if (!isAdded()) return;

        for (int i = 0; i < 5; i++) {
            if (!leftMatched[i]) {
                leftFailed[i] = true;
                leftButtons[i].setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(COLOR_FAILED));
                leftButtons[i].setAlpha(0.5f);
            }
        }

        boolean hasRemaining = false;
        for (boolean matched : leftMatched) {
            if (!matched) { hasRemaining = true; break; }
        }

        if (!hasRemaining) {
            finishRound();
            return;
        }

        isOpponentPhase = false;
        updateTurnLabel();
        showFeedback("Vi igrate preostale pojmove!", true);

        for (int i = 0; i < 5; i++) {
            if (!leftMatched[i]) {
                leftFailed[i] = false;
                leftButtons[i].setAlpha(1.0f);
                leftButtons[i].setEnabled(true);
                leftButtons[i].setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(COLOR_DEFAULT));
            }
        }

        sharedViewModel.startRoundTimer(30, this::finishRound);
    }

    private int countFailed() {
        int count = 0;
        for (boolean f : leftFailed) if (f) count++;
        return count;
    }

    private void finishRound() {
        if (!isAdded()) return;
        sharedViewModel.addCurrentPlayerPoints(myScore);
        handler.postDelayed(() -> {
            if (isAdded()) sharedViewModel.advanceGamePhase();
        }, 1000);
    }

    private void updateTurnLabel() {
        String runda = isRound1 ? "Runda 1" : "Runda 2";
        String igrac;
        if (isRound1) {
            igrac = isOpponentPhase ? "Protivnik igra preostale" : "Vi igrate";
        } else {
            igrac = isOpponentPhase ? "Protivnik igra" : "Vi igrate preostale";
        }
        tvSpojniceTurn.setText(runda + " — " + igrac);
    }

    private void onLeftClicked(int index) {
        if (isOpponentPhase) return;
        if (leftMatched[index]) return;
        if (leftFailed[index]) return;

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
        if (isOpponentPhase) return;
        if (rightMatched[rightIndex]) return;
        if (selectedLeftIndex < 0) return;

        boolean isCorrect = currentCorrect[selectedLeftIndex] == rightIndex;
        int prevLeft = selectedLeftIndex;

        if (isCorrect) {
            int matchColor = isOpponentPhase ? COLOR_OPPONENT : COLOR_CORRECT;
            leftButtons[prevLeft].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(matchColor));
            rightButtons[rightIndex].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(matchColor));
            leftButtons[prevLeft].setEnabled(false);
            rightButtons[rightIndex].setEnabled(false);
            leftMatched[prevLeft]    = true;
            rightMatched[rightIndex] = true;
            leftFailed[prevLeft]     = false;
            myScore += 2;
            matchedCount++;
            selectedLeftIndex = -1;

            showFeedback("+2 boda!", true);

            if (matchedCount == 5) {
                sharedViewModel.stopTimer();
                sharedViewModel.addCurrentPlayerPoints(myScore);
                handler.postDelayed(() -> {
                    if (isAdded()) sharedViewModel.advanceGamePhase();
                }, 1000);
            } else if (isOpponentPhase && matchedCount + countFailed() == 5) {
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
            selectedLeftIndex = -1;

            showFeedback("Nije tacno!", false);

            handler.postDelayed(() -> {
                if (!isAdded()) return;
                rightButtons[rightIndex].setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(COLOR_DEFAULT));
                if (matchedCount + countFailed() == 5) {
                    sharedViewModel.stopTimer();
                    handler.postDelayed(() -> {
                        if (isAdded()) onPlayerTimeUp();
                    }, 1500);
                }
            }, 700);
        }
    }

    private void showFeedback(String msg, boolean positive) {
        tvFeedback.setText(msg);
        tvFeedback.setTextColor(positive
                ? Color.parseColor("#2EC27E")
                : Color.parseColor("#F4A261"));
        tvFeedback.setVisibility(View.VISIBLE);
        handler.postDelayed(() -> {
            if (isAdded()) tvFeedback.setVisibility(View.INVISIBLE);
        }, 1500);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}