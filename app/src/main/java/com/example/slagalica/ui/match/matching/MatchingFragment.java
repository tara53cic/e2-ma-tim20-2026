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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.data.GameStateRepository;
import com.example.slagalica.data.UserStatsRepository;
import com.example.slagalica.domain.models.MatchingData;
import com.example.slagalica.ui.match.MatchViewModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchingFragment extends Fragment {

    private MatchingViewModel viewModel;
    private MatchViewModel sharedViewModel;
    private GameStateRepository gameStateRepo;
    private final UserStatsRepository statsRepo = new UserStatsRepository();
    private ListenerRegistration gameListener;
    private ListenerRegistration dataListener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Button[] leftButtons;
    private Button[] rightButtons;
    private TextView tvTitle, tvTurn, tvFeedback;

    private String[] currentLeft    = new String[5];
    private String[] currentRight   = new String[5];
    private int[]    currentCorrect = new int[5];

    private int selectedLeft = -1;
    private final boolean[] leftMatched  = new boolean[5];
    private final boolean[] rightMatched = new boolean[5];
    private final boolean[] starterUsed  = new boolean[5];
    private final boolean[] secondUsed   = new boolean[5];
    private final int[]     pairOwner    = new int[5];
    private int matchedCount   = 0;
    private int myMatchedCount = 0;
    private int myPointsThisRound = 0;

    private boolean isRound1;
    private boolean iAmStarter;
    private boolean roundDone  = false;
    private boolean dataLoaded = false;
    private boolean statsWritten = false;
    private String  localPhase = "";

    private String matchId, gameKey;

    private static final int C_DEFAULT  = Color.parseColor("#1E3A5F");
    private static final int C_SELECTED = Color.parseColor("#F4A261");
    private static final int C_MINE     = Color.parseColor("#E53935");
    private static final int C_OPP      = Color.parseColor("#508EFA");
    private static final int C_NONE     = Color.parseColor("#607D8B");
    private static final int C_FAILED   = Color.parseColor("#374A5E");
    private static final int C_WRONG    = Color.parseColor("#455A7A");

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_matching, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel       = new ViewModelProvider(this).get(MatchingViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);
        gameStateRepo   = new GameStateRepository();

        matchId = sharedViewModel.getMatchId();
        String fragPhase = sharedViewModel.getCurrentFragment().getValue();
        isRound1   = "SPOJNICE_R1".equals(fragPhase);
        gameKey    = isRound1 ? "matching_r1" : "matching_r2";
        iAmStarter = isRound1 ? sharedViewModel.getIsPlayer1() : !sharedViewModel.getIsPlayer1();

        tvTitle    = view.findViewById(R.id.tvSpojniceTitle);
        tvTurn     = view.findViewById(R.id.tvSpojniceTurn);
        tvFeedback = view.findViewById(R.id.tvSpojniceFeedback);

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

        setAllEnabled(false);
        tvTitle.setText("Učitavanje...");
        tvTurn.setText("");

        if (isRound1 && sharedViewModel.getIsPlayer1()) {
            viewModel.loadAndPublish(matchId);
            viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                if (loading == null || loading) return;
                onDataReady();
            });
        } else {
            waitForDataAndLoad();
        }
    }

    private void waitForDataAndLoad() {
        if (matchId == null) return;
        dataListener = FirebaseFirestore.getInstance()
                .collection("matches").document(matchId)
                .collection("games").document("matching_data")
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

        List<MatchingData> rounds = viewModel.getRounds().getValue();
        MatchingData data = null;
        if (rounds != null) {
            if (isRound1 && rounds.size() >= 1)       data = rounds.get(0);
            else if (!isRound1 && rounds.size() >= 2) data = rounds.get(1);
        }

        if (data == null || data.getLeft() == null
                || data.getRight() == null || data.getCorrectMap() == null) {
            tvTitle.setText("Greška: nema podataka!");
            Toast.makeText(getContext(), "Greška pri učitavanju spojnica!", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> l = data.getLeft();
        List<String> r = data.getRight();
        List<Long>   m = data.getCorrectMap();
        for (int i = 0; i < 5; i++) {
            if (i < l.size()) currentLeft[i]    = l.get(i);
            if (i < r.size()) currentRight[i]   = r.get(i);
            if (i < m.size()) currentCorrect[i] = m.get(i).intValue();
            leftButtons[i].setText(currentLeft[i]);
            rightButtons[i].setText(currentRight[i]);
            tint(leftButtons[i],  C_DEFAULT);
            tint(rightButtons[i], C_DEFAULT);
            final int idx = i;
            leftButtons[i].setOnClickListener(v  -> onLeft(idx));
            rightButtons[i].setOnClickListener(v -> onRight(idx));
        }
        tvTitle.setText(data.getTitle() != null ? data.getTitle().toUpperCase() : "SPOJNICE");
        dataLoaded = true;

        if (sharedViewModel.getIsPlayer1() && matchId != null) {
            Map<String, Object> init = new HashMap<>();
            init.put("phase", "STARTER");
            init.put("roundDone", false);
            gameStateRepo.set(matchId, gameKey, init);
        }

        startListening();
    }

    private void startListening() {
        if (matchId == null) return;
        gameListener = gameStateRepo.listen(matchId, gameKey, (snap, e) -> {
            if (e != null || snap == null || !snap.exists()) return;
            if (roundDone) return;

            for (int i = 0; i < 5; i++) {
                Boolean matched = snap.getBoolean("matched" + i);
                Long    ri      = snap.getLong("rightIndex" + i);
                Long    owner   = snap.getLong("owner" + i);
                if (Boolean.TRUE.equals(matched) && !leftMatched[i] && ri != null) {
                    int rIdx = ri.intValue();
                    int own  = owner != null ? owner.intValue() : 0;
                    leftMatched[i]     = true;
                    rightMatched[rIdx] = true;
                    starterUsed[i]     = true;
                    pairOwner[i]       = own;
                    int col = isMine(own) ? C_MINE : C_OPP;
                    tint(leftButtons[i],     col); leftButtons[i].setEnabled(false);
                    tint(rightButtons[rIdx], col); rightButtons[rIdx].setEnabled(false);
                    matchedCount++;
                }
                Boolean failed = snap.getBoolean("failed" + i);
                if (Boolean.TRUE.equals(failed) && !starterUsed[i] && !leftMatched[i] && !"SECOND".equals(localPhase)) {
                    starterUsed[i] = true;
                    tint(leftButtons[i], C_FAILED);
                    leftButtons[i].setAlpha(0.5f);
                    leftButtons[i].setEnabled(false);
                }
            }

            String newPhase = snap.getString("phase");
            if (newPhase != null && !newPhase.equals(localPhase)) {
                localPhase = newPhase;
                applyPhase();
            }

            Boolean done = snap.getBoolean("roundDone");
            if (Boolean.TRUE.equals(done) && !roundDone) {
                roundDone = true;
                writeStats();
                sharedViewModel.stopTimer();
                setAllEnabled(false);
                handler.postDelayed(() -> {
                    if (!isAdded()) return;
                    showResults(snap);
                    handler.postDelayed(() -> {
                        if (isAdded()) sharedViewModel.advanceGamePhase();
                    }, 3000);
                }, 300);
            }
        });
    }

    private void applyPhase() {
        if (!isAdded() || roundDone) return;
        String rnd = isRound1 ? "Runda 1" : "Runda 2";

        if ("STARTER".equals(localPhase)) {
            if (iAmStarter) {
                for (int i = 0; i < 5; i++) {
                    if (!leftMatched[i] && !starterUsed[i]) {
                        leftButtons[i].setEnabled(true);
                        leftButtons[i].setAlpha(1f);
                    }
                    if (!rightMatched[i]) rightButtons[i].setEnabled(true);
                }
                tvTurn.setText(rnd + " — Vi igrate");
            } else {
                setAllEnabled(false);
                tvTurn.setText(rnd + " — Protivnik igra");
            }
            sharedViewModel.startRoundTimer(30, () -> {
                if (!isAdded() || roundDone) return;
                if (iAmStarter) onStarterTimerUp();
            });

        } else if ("SECOND".equals(localPhase)) {
            boolean iAmSecond = !iAmStarter;
            if (iAmSecond) {
                for (int i = 0; i < 5; i++) {
                    if (!leftMatched[i]) {
                        leftButtons[i].setEnabled(true);
                        leftButtons[i].setAlpha(1f);
                        tint(leftButtons[i], C_DEFAULT);
                    }
                }
                for (int i = 0; i < 5; i++) {
                    if (!rightMatched[i]) {
                        rightButtons[i].setEnabled(true);
                        tint(rightButtons[i], C_DEFAULT);
                    }
                }
                tvTurn.setText(rnd + " — Vi igrate preostale");
                showFeedback("Vaš red — povežite preostale pojmove!", true);
            } else {
                setAllEnabled(false);
                tvTurn.setText(rnd + " — Protivnik igra preostale");
                showFeedback("Protivnik igra preostale pojmove...", false);
            }
            sharedViewModel.startRoundTimer(30, () -> {
                if (!isAdded() || roundDone) return;
                if (!iAmStarter) writeFinish();
            });
        }
    }

    private void onStarterTimerUp() {
        if (roundDone || !iAmStarter) return;
        boolean hasRemaining = false;
        for (boolean m : leftMatched) if (!m) { hasRemaining = true; break; }
        if (!hasRemaining) {
            writeFinish();
        } else {
            writePhase("SECOND");
        }
    }

    private void onLeft(int idx) {
        if (!canClick()) return;
        if (leftMatched[idx]) return;
        if ("STARTER".equals(localPhase) && starterUsed[idx]) return;
        if ("SECOND".equals(localPhase) && secondUsed[idx]) return;

        if (selectedLeft >= 0 && selectedLeft != idx)
            tint(leftButtons[selectedLeft], C_DEFAULT);
        if (selectedLeft == idx) {
            tint(leftButtons[idx], C_DEFAULT);
            selectedLeft = -1;
            return;
        }
        selectedLeft = idx;
        tint(leftButtons[idx], C_SELECTED);
    }

    private void onRight(int rightIdx) {
        if (!canClick() || rightMatched[rightIdx] || selectedLeft < 0) return;

        int prevLeft = selectedLeft;
        selectedLeft = -1;

        if (currentCorrect[prevLeft] == rightIdx) {
            int myNum = sharedViewModel.getIsPlayer1() ? 1 : 2;
            leftMatched[prevLeft]  = true;
            rightMatched[rightIdx] = true;
            starterUsed[prevLeft]  = true;
            pairOwner[prevLeft]    = myNum;
            matchedCount++;
            myMatchedCount++;
            myPointsThisRound += 2;

            tint(leftButtons[prevLeft],  C_MINE); leftButtons[prevLeft].setEnabled(false);
            tint(rightButtons[rightIdx], C_MINE); rightButtons[rightIdx].setEnabled(false);

            sharedViewModel.addCurrentPlayerPoints(2);
            showFeedback("+2 boda!", true);

            if (matchId != null) {
                Map<String, Object> u = new HashMap<>();
                u.put("matched" + prevLeft, true);
                u.put("rightIndex" + prevLeft, rightIdx);
                u.put("owner" + prevLeft, myNum);
                gameStateRepo.update(matchId, gameKey, u);
            }

            if (matchedCount == 5) { writeFinish(); return; }

            if (!iAmStarter && "SECOND".equals(localPhase) && allSecondUsed()) { writeFinish(); return; }

            if (iAmStarter && "STARTER".equals(localPhase) && allStarterUsed())
                onStarterTimerUp();

        } else {
            showFeedback("Nije tačno!", false);
            tint(rightButtons[rightIdx], C_WRONG);
            handler.postDelayed(() -> {
                if (isAdded()) tint(rightButtons[rightIdx], C_DEFAULT);
            }, 700);

            if (iAmStarter && "STARTER".equals(localPhase)) {
                starterUsed[prevLeft] = true;
                tint(leftButtons[prevLeft], C_FAILED);
                leftButtons[prevLeft].setAlpha(0.5f);
                leftButtons[prevLeft].setEnabled(false);
                if (matchId != null) {
                    Map<String, Object> u = new HashMap<>();
                    u.put("failed" + prevLeft, true);
                    gameStateRepo.update(matchId, gameKey, u);
                }
                if (allStarterUsed()) onStarterTimerUp();
            }
            if (!iAmStarter && "SECOND".equals(localPhase)) {
                secondUsed[prevLeft] = true;
                tint(leftButtons[prevLeft], C_FAILED);
                leftButtons[prevLeft].setAlpha(0.5f);
                leftButtons[prevLeft].setEnabled(false);
                if (allSecondUsed()) writeFinish();
            }
        }
    }

    private void writePhase(String phase) {
        if (matchId == null) return;
        Map<String, Object> u = new HashMap<>();
        u.put("phase", phase);
        gameStateRepo.update(matchId, gameKey, u);
    }

    private void writeFinish() {
        if (roundDone || matchId == null) return;
        Map<String, Object> u = new HashMap<>();
        u.put("phase", "DONE");
        u.put("roundDone", true);
        gameStateRepo.update(matchId, gameKey, u);
    }

    private void writeStats() {
        if (statsWritten) return;
        statsWritten = true;
        String uid = statsRepo.getCurrentUid();
        if (uid != null) {
            statsRepo.recordSpojnice(uid, myMatchedCount, 5, myPointsThisRound);
        }
    }

    private boolean canClick() {
        if (!dataLoaded || roundDone || localPhase.isEmpty()) return false;
        if ("STARTER".equals(localPhase)) return iAmStarter;
        if ("SECOND".equals(localPhase))  return !iAmStarter;
        return false;
    }

    private boolean allStarterUsed() {
        for (int i = 0; i < 5; i++) if (!starterUsed[i] && !leftMatched[i]) return false;
        return true;
    }

    private boolean allSecondUsed() {
        for (int i = 0; i < 5; i++) if (!secondUsed[i] && !leftMatched[i]) return false;
        return true;
    }

    private boolean isMine(int own) {
        return (sharedViewModel.getIsPlayer1() && own == 1)
                || (!sharedViewModel.getIsPlayer1() && own == 2);
    }

    private void showResults(DocumentSnapshot snap) {
        if (!isAdded()) return;
        setAllEnabled(false);
        tvTurn.setText("Runda završena!");
        tvFeedback.setVisibility(View.VISIBLE);
        tvFeedback.setTextColor(Color.WHITE);
        tvFeedback.setText("Tačni parovi:");

        for (int i = 0; i < 5; i++) {
            rightButtons[i].setText(currentRight[currentCorrect[i]]);
            if (leftMatched[i]) {
                int col = isMine(pairOwner[i]) ? C_MINE : C_OPP;
                tint(leftButtons[i],  col); leftButtons[i].setAlpha(1f);
                tint(rightButtons[i], col); rightButtons[i].setAlpha(1f);
            } else {
                tint(leftButtons[i],  C_NONE); leftButtons[i].setAlpha(0.7f);
                tint(rightButtons[i], C_NONE); rightButtons[i].setAlpha(0.7f);
            }
        }
    }

    private void showFeedback(String msg, boolean ok) {
        tvFeedback.setText(msg);
        tvFeedback.setTextColor(ok
                ? Color.parseColor("#2EC27E") : Color.parseColor("#F4A261"));
        tvFeedback.setVisibility(View.VISIBLE);
        handler.postDelayed(() -> {
            if (isAdded()) tvFeedback.setVisibility(View.INVISIBLE);
        }, 1500);
    }

    private void tint(Button b, int color) {
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void setAllEnabled(boolean en) {
        for (Button b : leftButtons)  if (b != null) b.setEnabled(en);
        for (Button b : rightButtons) if (b != null) b.setEnabled(en);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (gameListener != null) gameListener.remove();
        if (dataListener != null) dataListener.remove();
    }
}