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
import com.example.slagalica.data.GameStateRepository;
import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.UserStatsRepository;
import com.example.slagalica.domain.service.GameStateMonitor;
import com.example.slagalica.ui.match.MatchViewModel;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkockoFragment extends Fragment {

    private SkockoViewModel viewModel;
    private MatchViewModel sharedViewModel;
    private GameStateRepository gameStateRepo;
    private final UserStatsRepository statsRepo = new UserStatsRepository();
    private ListenerRegistration gameListener;
    private GameStateMonitor gameStateMonitor;
    private java.util.Timer abandonmentTimer;
    private String opponentUserId;
    private boolean opponentAbandoned = false;

    private String matchId, gameKey;
    private boolean isGuesser;
    private boolean localRoundDone = false;
    private boolean statsWritten   = false;

    private final String[] challengerInput = new String[4];
    private int challengerInputIdx = 0;
    private boolean challengerPhaseActive = false;

    private boolean mainTimerStarted = false;
    private boolean challengerTimerStarted = false;

    private TextView tvRoundInfo;
    private TextView[][] cells;
    private TextView[][] results;
    private TextView[] opponentCells;
    private TextView[] opponentResults;
    private Button btnSkocko, btnClub, btnSpade, btnHeart, btnTriangle, btnStar, btnConfirm;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final int COLOR_CORRECT_PLACE = Color.parseColor("#2EC27E");
    private static final int COLOR_WRONG_PLACE   = Color.parseColor("#F4A261");
    private static final int COLOR_EMPTY_RESULT  = Color.parseColor("#020617");
    private static final int COLOR_CHALLENGER    = Color.parseColor("#508EFA");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_skocko, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SkockoViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);
        gameStateRepo = new GameStateRepository();
        gameStateMonitor = new GameStateMonitor();

        matchId = sharedViewModel.getMatchId();
        String phase = sharedViewModel.getCurrentFragment().getValue();
        gameKey = "SKOCKO_R1".equals(phase) ? "skocko_r1" : "skocko_r2";
        isGuesser = "skocko_r1".equals(gameKey)
                ? sharedViewModel.getIsPlayer1()
                : !sharedViewModel.getIsPlayer1();

        connectViews(view);
        setupCellClicks();
        setupSymbolButtons();
        btnConfirm.setOnClickListener(v -> onConfirmClicked());

        if (sharedViewModel.isChallenge()) {
            opponentAbandoned = false;
        } else {
            if (Boolean.TRUE.equals(sharedViewModel.getIsOpponentAbandoned().getValue())) {
                opponentAbandoned = true;
            } else {
                startAbandonmentMonitoring();
            }
            // Trenutna reakcija na osnovu match dokumenta (vidi MatchViewModel), ne čekamo
            // isključivo spori online/inGame polling.
            sharedViewModel.getIsOpponentAbandoned().observe(getViewLifecycleOwner(), abandoned -> {
                if (Boolean.TRUE.equals(abandoned)) onOpponentConfirmedGone();
            });
        }

        if (sharedViewModel.isChallenge()) {
            isGuesser = true;
            tvRoundInfo.setText("Pogodi kombinaciju!");
            startMainTimerIfNeeded(true, false);
            enableButtons();
        } else {
            if (matchId != null && (isGuesser || opponentAbandoned)) {
                // Ako je protivnik (koji je trebalo da bude pogađač u ovoj rundi) već
                // napustio partiju, odmah označavamo njegov potez kao završen (neuspešan)
                // kako bismo bez čekanja prešli na fazu prisutnog igrača (izazivača).
                boolean skipAbsentGuesserTurn = opponentAbandoned && !isGuesser;
                Map<String, Object> data = new HashMap<>();
                data.put("secretCombination", Arrays.asList(viewModel.secretCombination));
                data.put("playerTurnDone", skipAbsentGuesserTurn);
                data.put("playerSolved", false);
                data.put("roundFinished", false);
                gameStateRepo.set(matchId, gameKey, data);
            }
            listenToGameState();
            disableButtons();
        }

        if (isGuesser) {
            tvRoundInfo.setText("Tvoja partija – 6 pokušaja");
        } else {
            tvRoundInfo.setText("Protivnik igra – čeka se...");
        }
    }

    private void listenToGameState() {
        if (matchId == null) return;

        gameListener = gameStateRepo.listen(matchId, gameKey, (snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;

            List<String> secret = (List<String>) snapshot.get("secretCombination");

            if (secret != null && secret.size() == 4) {
                for (int i = 0; i < 4; i++) {
                    viewModel.secretCombination[i] = secret.get(i);
                }
            }

            Boolean turnDone = snapshot.getBoolean("playerTurnDone");
            Boolean solved = snapshot.getBoolean("playerSolved");
            Boolean finished = snapshot.getBoolean("roundFinished");

            startMainTimerIfNeeded(
                    Boolean.TRUE.equals(turnDone),
                    Boolean.TRUE.equals(finished)
            );

            for (int row = 0; row < 6; row++) {
                List<String> attempt = (List<String>) snapshot.get("attempt_" + row);
                List<Long> result = (List<Long>) snapshot.get("result_" + row);

                if (attempt != null && result != null) {
                    for (int c = 0; c < 4; c++) {
                        cells[row][c].setText(attempt.get(c));
                        cells[row][c].setTextColor(Color.WHITE);
                        cells[row][c].setGravity(android.view.Gravity.CENTER);
                    }

                    showResult(
                            results[row],
                            new int[]{
                                    result.get(0).intValue(),
                                    result.get(1).intValue()
                            }
                    );
                }
            }

            if (Boolean.TRUE.equals(turnDone)
                    && !Boolean.TRUE.equals(solved)
                    && !isGuesser
                    && !challengerPhaseActive
                    && !localRoundDone) {

                startChallengerPhase();
            }

            List<String> cGuess = (List<String>) snapshot.get("challengerGuess");
            List<Long> cResult = (List<Long>) snapshot.get("challengerResult");

            if (cGuess != null && cResult != null && isGuesser && !localRoundDone) {
                showChallengerAttemptUI(
                        cGuess,
                        new int[]{
                                cResult.get(0).intValue(),
                                cResult.get(1).intValue()
                        }
                );
            }

            if (Boolean.TRUE.equals(finished) && !localRoundDone) {
                localRoundDone = true;
                if (abandonmentTimer != null) {
                    abandonmentTimer.cancel();
                    abandonmentTimer = null;
                }
                stopTimer();
                disableButtons();

                handler.postDelayed(() -> {
                    if (isAdded()) {
                        sharedViewModel.advanceGamePhase();
                    }
                }, 1500);
            }
        });
    }

    private void startMainTimerIfNeeded(boolean playerTurnDone, boolean roundFinished) {
        if (mainTimerStarted || localRoundDone || playerTurnDone || roundFinished) {
            return;
        }

        mainTimerStarted = true;

        sharedViewModel.stopTimer();
        sharedViewModel.startRoundTimer(
                30,
                isGuesser ? this::onGuesserTimerUp : () -> {}
        );

        if (isGuesser) {
            enableButtons();
            tvRoundInfo.setText("Tvoja partija – 6 pokušaja");
        } else {
            disableButtons();
            tvRoundInfo.setText("Protivnik igra – čeka se...");
        }
    }

    private void onGuesserTimerUp() {
        if (localRoundDone || !isGuesser) return;
        viewModel.playerTurnFinished = true;
        disableButtons();
        tvRoundInfo.setText("Vreme isteklo! Protivnik ima 10 sekundi.");
        writeGuesserStats(false, -1, 0);
        writeGuesserTurnDone(false);
        if (opponentAbandoned) finalizeRoundNoChallenger();
    }

    private void onConfirmClicked() {
        if (challengerPhaseActive) {
            confirmChallengerAttempt();
        } else if (isGuesser) {
            confirmGuesserAttempt();
        }
    }

    private void confirmGuesserAttempt() {
        if (viewModel.playerTurnFinished || viewModel.roundFinished || localRoundDone) return;
        if (!viewModel.isInputFull()) {
            Toast.makeText(requireContext(), "Unesi 4 znaka.", Toast.LENGTH_SHORT).show();
            return;
        }

        int[] result = viewModel.checkCurrentInput();
        int row = viewModel.currentAttempt;
        showResult(results[row], result);

        if (matchId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("attempt_" + row, Arrays.asList(viewModel.currentInput.clone()));
            updates.put("result_" + row, Arrays.asList(result[0], result[1]));
            gameStateRepo.update(matchId, gameKey, updates);
        }

        if (viewModel.isSolved(result)) {
            int points = viewModel.calculatePlayerPoints();
            tvRoundInfo.setText("Pogodio/la si! +" + points + " bodova");
            disableButtons();
            stopTimer();
            writeGuesserStats(true, viewModel.currentAttempt, points);
            sharedViewModel.addCurrentPlayerPoints(points);

            if (sharedViewModel.isChallenge()) {
                localRoundDone = true;
                handler.postDelayed(() -> {
                    if (isAdded()) sharedViewModel.advanceGamePhase();
                }, 1500);
                return;
            }

            if (matchId != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("playerTurnDone", true);
                updates.put("playerSolved", true);
                updates.put("playerPoints", points);
                updates.put("roundFinished", true);
                gameStateRepo.update(matchId, gameKey, updates);
            }
            return;
        }

        viewModel.currentAttempt++;
        viewModel.clearInput();

        if (viewModel.currentAttempt >= 6) {
            viewModel.playerTurnFinished = true;
            disableButtons();
            stopTimer();
            tvRoundInfo.setText("Nisi pogodio/la. Protivnik ima 10 sekundi.");
            writeGuesserStats(false, -1, 0);
            writeGuesserTurnDone(false);
            if (opponentAbandoned) finalizeRoundNoChallenger();
        }
    }

    private void finalizeRoundNoChallenger() {
        // Izazivač (protivnik) je napustio partiju - ne čekamo njegov pokušaj od 10 sekundi.
        if (matchId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("challengerPoints", 0);
        updates.put("roundFinished", true);
        gameStateRepo.update(matchId, gameKey, updates);
    }

    private void writeGuesserStats(boolean solved, int solvedAtAttempt, int points) {
        if (statsWritten || !isGuesser) return;
        statsWritten = true;
        String uid = statsRepo.getCurrentUid();
        if (uid != null) {
            statsRepo.recordSkocko(uid, solvedAtAttempt, points);
        }
    }

    private void writeGuesserTurnDone(boolean solved) {
        if (sharedViewModel.isChallenge()) {
            localRoundDone = true;
            writeGuesserStats(solved, viewModel.currentAttempt, viewModel.calculatePlayerPoints());
            sharedViewModel.advanceGamePhase();
            return;
        }
        if (matchId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("playerTurnDone", true);
        updates.put("playerSolved", solved);
        gameStateRepo.update(matchId, gameKey, updates);
    }

    private void startChallengerPhase() {
        if (challengerTimerStarted || localRoundDone) {
            return;
        }

        challengerTimerStarted = true;
        challengerPhaseActive = true;

        tvRoundInfo.setText("Tvoj pokušaj – 10 sekundi!");
        enableButtons();

        for (int i = 0; i < 4; i++) {
            opponentCells[i].setText("");
            opponentCells[i].setBackgroundTintList(ColorStateList.valueOf(COLOR_CHALLENGER));
        }

        sharedViewModel.stopTimer();
        sharedViewModel.startRoundTimer(10, this::onChallengerTimerUp);
    }

    private void onChallengerTimerUp() {
        if (localRoundDone || !challengerPhaseActive) return;
        challengerPhaseActive = false;
        disableButtons();
        tvRoundInfo.setText("Vreme isteklo! Niko nije pogodio kombinaciju.");
        if (matchId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("challengerPoints", 0);
            updates.put("roundFinished", true);
            gameStateRepo.update(matchId, gameKey, updates);
        }
    }

    private void confirmChallengerAttempt() {
        if (challengerInputIdx < 4) {
            Toast.makeText(requireContext(), "Unesi 4 znaka.", Toast.LENGTH_SHORT).show();
            return;
        }

        int[] result = viewModel.checkCombination(challengerInput);
        showResult(opponentResults, result);

        boolean solved = viewModel.isSolved(result);
        int points = solved ? 10 : 0;

        challengerPhaseActive = false;
        disableButtons();
        stopTimer();

        if (solved) {
            tvRoundInfo.setText("Pogodio/la si! +10 bodova");
            sharedViewModel.addCurrentPlayerPoints(10);
        } else {
            tvRoundInfo.setText("Nije tačno. Niko nije pogodio kombinaciju.");
        }

        if (matchId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("challengerGuess",  Arrays.asList(challengerInput.clone()));
            updates.put("challengerResult", Arrays.asList(result[0], result[1]));
            updates.put("challengerPoints", points);
            updates.put("roundFinished", true);
            gameStateRepo.update(matchId, gameKey, updates);
        }
    }

    private void addSymbol(String symbol) {
        if (localRoundDone) return;
        if (challengerPhaseActive) {
            if (challengerInputIdx >= 4) return;
            challengerInput[challengerInputIdx] = symbol;
            opponentCells[challengerInputIdx].setText(symbol);
            opponentCells[challengerInputIdx].setTextColor(Color.WHITE);
            opponentCells[challengerInputIdx].setGravity(android.view.Gravity.CENTER);
            challengerInputIdx++;
        } else if (isGuesser && !viewModel.playerTurnFinished && !viewModel.roundFinished) {
            boolean added = viewModel.addSymbol(symbol);
            if (!added) return;
            int row = viewModel.currentAttempt;
            int col = viewModel.currentInputIndex - 1;
            cells[row][col].setText(symbol);
            cells[row][col].setTextColor(Color.WHITE);
            cells[row][col].setGravity(android.view.Gravity.CENTER);
        }
    }

    private void showChallengerAttemptUI(List<String> guess, int[] result) {
        for (int i = 0; i < 4; i++) {
            opponentCells[i].setText(guess.get(i));
            opponentCells[i].setTextColor(Color.WHITE);
            opponentCells[i].setGravity(android.view.Gravity.CENTER);
            opponentCells[i].setBackgroundTintList(ColorStateList.valueOf(COLOR_CHALLENGER));
        }
        showResult(opponentResults, result);
        boolean solved = result[0] == 4;
        tvRoundInfo.setText(solved ? "Protivnik je pogodio! +10 bodova" : "Protivnik nije pogodio. Niko nije pogodio.");
    }

    private void showResult(TextView[] views, int[] result) {
        int idx = 0;
        for (int i = 0; i < result[0]; i++)
            views[idx++].setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT_PLACE));
        for (int i = 0; i < result[1]; i++)
            views[idx++].setBackgroundTintList(ColorStateList.valueOf(COLOR_WRONG_PLACE));
        while (idx < 4)
            views[idx++].setBackgroundTintList(ColorStateList.valueOf(COLOR_EMPTY_RESULT));
    }

    private void setupCellClicks() {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                final int r = row, c = col;
                cells[r][c].setOnClickListener(v -> {
                    if (!isGuesser || challengerPhaseActive || viewModel.playerTurnFinished || localRoundDone) return;
                    if (r == viewModel.currentAttempt) removeSymbolAt(c);
                });
            }
        }
    }

    private void removeSymbolAt(int col) {
        if (col >= viewModel.currentInputIndex) return;
        boolean removed = viewModel.removeSymbolAt(col);
        if (!removed) return;
        int row = viewModel.currentAttempt;
        for (int c = 0; c < 4; c++) {
            cells[row][c].setText(c < viewModel.currentInputIndex ? viewModel.currentInput[c] : "");
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
                view.findViewById(R.id.opCell0), view.findViewById(R.id.opCell1),
                view.findViewById(R.id.opCell2), view.findViewById(R.id.opCell3)
        };
        opponentResults = new TextView[]{
                view.findViewById(R.id.opRes0), view.findViewById(R.id.opRes1),
                view.findViewById(R.id.opRes2), view.findViewById(R.id.opRes3)
        };
        btnSkocko   = view.findViewById(R.id.btnSkocko);
        btnClub     = view.findViewById(R.id.btnClub);
        btnSpade    = view.findViewById(R.id.btnSpade);
        btnHeart    = view.findViewById(R.id.btnHeart);
        btnTriangle = view.findViewById(R.id.btnTriangle);
        btnStar     = view.findViewById(R.id.btnStar);
        btnConfirm  = view.findViewById(R.id.btnConfirm);
    }

    private void setupSymbolButtons() {
        btnSkocko.setOnClickListener(v -> addSymbol("☻"));
        btnClub.setOnClickListener(v -> addSymbol("♣"));
        btnSpade.setOnClickListener(v -> addSymbol("♠"));
        btnHeart.setOnClickListener(v -> addSymbol("♥"));
        btnTriangle.setOnClickListener(v -> addSymbol("▲"));
        btnStar.setOnClickListener(v -> addSymbol("★"));
    }

    private void enableButtons() {
        btnSkocko.setEnabled(true); btnClub.setEnabled(true);
        btnSpade.setEnabled(true);  btnHeart.setEnabled(true);
        btnTriangle.setEnabled(true); btnStar.setEnabled(true);
        btnConfirm.setEnabled(true);
    }

    private void disableButtons() {
        btnSkocko.setEnabled(false); btnClub.setEnabled(false);
        btnSpade.setEnabled(false);  btnHeart.setEnabled(false);
        btnTriangle.setEnabled(false); btnStar.setEnabled(false);
        btnConfirm.setEnabled(false);
    }

    private void stopTimer() { sharedViewModel.stopTimer(); }

    private void startAbandonmentMonitoring() {
        if (matchId == null || opponentAbandoned) return;
        loadOpponentUserId();

        if (abandonmentTimer != null) abandonmentTimer.cancel();
        abandonmentTimer = gameStateMonitor.startAbandonmentWatch(
                () -> opponentUserId,
                () -> {
                    if (!isAdded() || localRoundDone || opponentAbandoned) return;
                    opponentAbandoned = true;
                    handleOpponentAbandonment();
                }
        );
    }

    private void loadOpponentUserId() {
        if (matchId == null) return;
        new MatchRepository().getMatch(matchId).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                boolean isP1 = sharedViewModel.getIsPlayer1();
                opponentUserId = isP1 ? doc.getString("player2_id") : doc.getString("player1_id");
            }
        });
    }

    private void handleOpponentAbandonment() {
        if (localRoundDone) return;
        
        if (matchId != null && opponentUserId != null) {
            gameStateMonitor.detectAndHandleAbandonment(
                    matchId,
                    opponentUserId,
                    com.google.firebase.auth.FirebaseAuth.getInstance().getUid()
            ).addOnSuccessListener(wasAbandoned -> {
                if (Boolean.TRUE.equals(wasAbandoned)) {
                    sharedViewModel.setOpponentAbandoned(true);
                    onOpponentConfirmedGone();
                } else {
                    opponentAbandoned = false;
                    enableButtons();
                    startAbandonmentMonitoring();
                }
            });
        }
    }

    private void onOpponentConfirmedGone() {
        if (abandonmentTimer != null) {
            abandonmentTimer.cancel();
            abandonmentTimer = null;
        }
        boolean firstTime = !opponentAbandoned;
        opponentAbandoned = true;
        if (firstTime && isAdded()) {
            Toast.makeText(getContext(), "Protivnik je napustio igru!", Toast.LENGTH_SHORT).show();
        }
        checkAndSkipOpponentTurn();
    }

    private void checkAndSkipOpponentTurn() {
        if (localRoundDone) return;
        if (!isGuesser) {
            // Runda pripada protivniku (pogađaču) koji je napustio partiju - ako još
            // nisam ušao u svoju fazu izazivača, lažiramo njegov "nije pogodio" signal
            // kako bih odmah dobio priliku (ne čekamo protivnikovih 30 sekundi uzalud).
            if (!challengerPhaseActive) {
                writeGuesserTurnDone(false);
            }
        } else if (viewModel.playerTurnFinished) {
            // Ja sam pogađač i već sam završio svoj potez; izazivač (protivnik) je
            // napustio partiju pa ne čekamo njegovih 10 sekundi.
            finalizeRoundNoChallenger();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimer();
        handler.removeCallbacksAndMessages(null);
        if (gameListener != null) gameListener.remove();
        if (abandonmentTimer != null) {
            abandonmentTimer.cancel();
            abandonmentTimer = null;
        }
    }
}