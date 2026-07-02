package com.example.slagalica.ui.match.number_game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.data.NumberGameRepository;
import com.example.slagalica.data.UserStatsRepository;
import com.example.slagalica.domain.service.GameStateMonitor;
import com.example.slagalica.domain.service.NumberGameScoringService;
import com.example.slagalica.domain.usecase.EvaluateMathExpressionUseCase;
import com.example.slagalica.ui.match.MatchViewModel;
import com.example.slagalica.utils.ShakeDetector;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class NumberGameFragment extends Fragment {

    private NumberGameViewModel viewModel;
    private MatchViewModel sharedViewModel;
    private EvaluateMathExpressionUseCase evaluateMathExpressionUseCase;
    private NumberGameScoringService scoringService;
    private final UserStatsRepository statsRepo = new UserStatsRepository();
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;
    private NumberGameRepository numberGameRepo;
    private ListenerRegistration gameListener;
    private GameStateMonitor gameStateMonitor;  // ← DODANO

    private String matchId;
    private String gameKey;
    private boolean isActivePlayer;
    private boolean initialTimerStarted = false;
    private boolean statsWritten = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_number_game, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(NumberGameViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(MatchViewModel.class);
        evaluateMathExpressionUseCase = new EvaluateMathExpressionUseCase();
        scoringService = new NumberGameScoringService();
        numberGameRepo = new NumberGameRepository();
        gameStateMonitor = new GameStateMonitor();  // ← DODANO

        matchId = sharedViewModel.getMatchId();
        String phase = sharedViewModel.getCurrentFragment().getValue();
        boolean isRound1 = "MOJ_BROJ_R1".equals(phase);
        gameKey = isRound1 ? "number_game_r1" : "number_game_r2";
        isActivePlayer = isRound1 ? sharedViewModel.getIsPlayer1() : !sharedViewModel.getIsPlayer1();

        TextView tvTargetNumber = view.findViewById(R.id.tvTargetNumber);
        TextView tvCalcScreen = view.findViewById(R.id.tvCalcScreen);
        MaterialButton btnStopTarget = view.findViewById(R.id.btnStopTarget);

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            shakeDetector = new ShakeDetector();
            shakeDetector.setOnShakeListener(() -> {
                if (!isActivePlayer) return;
                NumberGameViewModel.GameState state = viewModel.getGameState().getValue();
                if (state == NumberGameViewModel.GameState.SHUFFLE_TARGET || state == NumberGameViewModel.GameState.SHUFFLE_NUMBERS) {
                    handleStopClicked();
                }
            });
        }

        List<MaterialButton> numberButtons = new ArrayList<>();
        numberButtons.add(view.findViewById(R.id.btnNum1));
        numberButtons.add(view.findViewById(R.id.btnNum2));
        numberButtons.add(view.findViewById(R.id.btnNum3));
        numberButtons.add(view.findViewById(R.id.btnNum4));
        numberButtons.add(view.findViewById(R.id.btnNumMid));
        numberButtons.add(view.findViewById(R.id.btnNumLarge));

        viewModel.getTargetNumber().observe(getViewLifecycleOwner(), tvTargetNumber::setText);

        viewModel.getSmallNumbers().observe(getViewLifecycleOwner(), numbers -> {
            for (int i = 0; i < numberButtons.size() && i < numbers.size(); i++) {
                numberButtons.get(i).setText(numbers.get(i));
            }
        });

        viewModel.getGameState().observe(getViewLifecycleOwner(), state -> {
            if (state == NumberGameViewModel.GameState.SHUFFLE_TARGET) {
                btnStopTarget.setVisibility(isActivePlayer ? View.VISIBLE : View.GONE);
                if (matchId == null) {
                    sharedViewModel.startRoundTimer(5, () -> {
                        if (isActivePlayer) handleStopClicked();
                    });
                }
            } else if (state == NumberGameViewModel.GameState.PLAYING) {
                btnStopTarget.setVisibility(View.GONE);
                sharedViewModel.startRoundTimer(60, () -> submitGameResult());
            } else if (state == NumberGameViewModel.GameState.FINISHED) {
                btnStopTarget.setVisibility(View.GONE);
            }
        });

        viewModel.getCurrentNumberIndex().observe(getViewLifecycleOwner(), index -> {
            if (index >= 0 && viewModel.getGameState().getValue() == NumberGameViewModel.GameState.SHUFFLE_NUMBERS) {
                btnStopTarget.setVisibility(isActivePlayer ? View.VISIBLE : View.GONE);
                sharedViewModel.startRoundTimer(5, () -> {
                    if (isActivePlayer) handleStopClicked();
                });
            }
        });

        viewModel.getUsedNumbers().observe(getViewLifecycleOwner(), usedList -> {
            for (int i = 0; i < numberButtons.size(); i++) {
                numberButtons.get(i).setEnabled(!usedList.get(i));
            }
        });

        viewModel.getCurrentExpression().observe(getViewLifecycleOwner(), tvCalcScreen::setText);

        btnStopTarget.setOnClickListener(v -> {
            if (isActivePlayer) handleStopClicked();
        });

        for (int i = 0; i < numberButtons.size(); i++) {
            final int index = i;
            numberButtons.get(i).setOnClickListener(v -> {
                viewModel.onNumberClicked(index, numberButtons.get(index).getText().toString());
            });
        }

        int[] opIds = {R.id.btnOpPlus, R.id.btnOpMinus, R.id.btnOpMul, R.id.btnOpDiv, R.id.btnOpLeftParen, R.id.btnOpRightParen};
        String[] opChars = {"+", "-", "*", "/", "(", ")"};
        for (int i = 0; i < opIds.length; i++) {
            final String op = opChars[i];
            view.findViewById(opIds[i]).setOnClickListener(v -> viewModel.onOperatorClicked(op));
        }

        view.findViewById(R.id.btnBackspace).setOnClickListener(v -> viewModel.onBackspace());
        view.findViewById(R.id.btnClearAll).setOnClickListener(v -> viewModel.onClearAll());
        view.findViewById(R.id.btnConfirmCalc).setOnClickListener(v -> submitGameResult());

        if (matchId != null) {
            if (sharedViewModel.getIsOpponentAbandoned().getValue() != null && sharedViewModel.getIsOpponentAbandoned().getValue()) {
                opponentAbandoned = true;
                writeOpponentDummyResult();
            }

            if (sharedViewModel.isChallenge()) {
                isActivePlayer = true;
                viewModel.startTargetShuffle();
                sharedViewModel.startRoundTimer(5, () -> {
                    handleStopClicked();
                });
            } else if (isActivePlayer) {
                long now = System.currentTimeMillis();
                numberGameRepo.initRound(matchId, gameKey, now).addOnSuccessListener(v -> {
                    viewModel.startTargetShuffle();
                    sharedViewModel.startRoundTimer(5, () -> {
                        if (isActivePlayer) handleStopClicked();
                    });
                    listenToGameState();
                    // ← DODANO: Proveravamo da li je protivnik aktivan
                    checkOpponentStatus();
                });
            } else {
                viewModel.startTargetShuffle();
                listenToGameState();
                // ← DODANO: Proveravamo da li je protivnik aktivan (aktivni igrač)
                checkOpponentStatus();
            }
        } else {
            viewModel.startTargetShuffle();
        }
    }

    private void handleStopClicked() {
        NumberGameViewModel.GameState state = viewModel.getGameState().getValue();
        if (state == NumberGameViewModel.GameState.SHUFFLE_TARGET) {
            String locked = viewModel.peekTarget();
            viewModel.onStopClicked();
            if (matchId != null) numberGameRepo.lockTarget(matchId, gameKey, locked);
        } else if (state == NumberGameViewModel.GameState.SHUFFLE_NUMBERS) {
            int slot = viewModel.getCurrentNumberIndex().getValue() != null
                    ? viewModel.getCurrentNumberIndex().getValue() : 0;
            String locked = viewModel.peekNumber(slot);
            viewModel.onStopClicked();
            if (matchId != null) numberGameRepo.lockNumber(matchId, gameKey, slot, locked);
        }
    }

    private void listenToGameState() {
        gameListener = numberGameRepo.listen(matchId, gameKey, (snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;

            Long lockPhase = snapshot.getLong("lockPhase");
            Long startTime = snapshot.getLong("startTime");
            if (lockPhase == null) return;

            if (!isActivePlayer) {
                if (lockPhase == 0 && startTime != null && !initialTimerStarted) {
                    initialTimerStarted = true;
                    long elapsed = System.currentTimeMillis() - startTime;
                    int remaining = (int) (5 - (elapsed / 1000));
                    if (remaining > 0) {
                        sharedViewModel.startRoundTimer(remaining, () -> {});
                    }
                }

                NumberGameViewModel.GameState state = viewModel.getGameState().getValue();

                if (lockPhase >= 1 && state == NumberGameViewModel.GameState.SHUFFLE_TARGET) {
                    String target = snapshot.getString("targetNumber");
                    if (target != null) viewModel.forceTarget(target);
                }

                for (int slot = 0; slot < 6; slot++) {
                    if (lockPhase >= slot + 2) {
                        String val = snapshot.getString("num" + slot);
                        Integer curIndex = viewModel.getCurrentNumberIndex().getValue();
                        NumberGameViewModel.GameState curState = viewModel.getGameState().getValue();
                        if (val != null && !val.isEmpty()
                                && curState == NumberGameViewModel.GameState.SHUFFLE_NUMBERS
                                && curIndex != null && curIndex == slot) {
                            viewModel.forceNumber(slot, val);
                        }
                    }
                }
            }

            Boolean p1sub = snapshot.getBoolean("p1Submitted");
            Boolean p2sub = snapshot.getBoolean("p2Submitted");
            boolean oppAbandonedGlobal = Boolean.TRUE.equals(sharedViewModel.getIsOpponentAbandoned().getValue());

            boolean bothInOrAbandoned = (Boolean.TRUE.equals(p1sub) && Boolean.TRUE.equals(p2sub))
                    || (oppAbandonedGlobal && (Boolean.TRUE.equals(p1sub) || Boolean.TRUE.equals(p2sub)));

            if (bothInOrAbandoned && !resultFinalized) {
                resultFinalized = true;

                Long p1res = snapshot.getLong("p1Result");
                Long p2res = snapshot.getLong("p2Result");
                long myResult       = sharedViewModel.getIsPlayer1() ? (p1res != null ? p1res : 0L) : (p2res != null ? p2res : 0L);
                long opponentResult = sharedViewModel.getIsPlayer1() ? (p2res != null ? p2res : 0L) : (p1res != null ? p1res : 0L);

                String targetStr = viewModel.getTargetNumber().getValue();
                long targetLong = targetStr != null && !targetStr.equals("---") ? Long.parseLong(targetStr) : 0;

                int points = scoringService.calculatePoints(targetLong, myResult, opponentResult, isActivePlayer);

                writeStats(myResult == targetLong, points);

                sharedViewModel.addCurrentPlayerPoints(points);
                Toast.makeText(getContext(), getString(R.string.game_result_toast, myResult, points), Toast.LENGTH_LONG).show();
                sharedViewModel.advanceGamePhase();
            }
        });
    }

    private void writeStats(boolean exact, int points) {
        if (statsWritten) return;
        statsWritten = true;
        String uid = statsRepo.getCurrentUid();
        if (uid != null) {
            statsRepo.recordMojBroj(uid, exact, points);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null && shakeDetector != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null && shakeDetector != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (gameListener != null) gameListener.remove();
        if (abandonmentTimer != null) {
            abandonmentTimer.cancel();
            abandonmentTimer = null;
        }
    }

    private boolean resultFinalized = false;
    private String opponentUserId;
    private boolean opponentAbandoned = false;
    private java.util.Timer abandonmentTimer;

    private void submitGameResult() {
        if (viewModel.getGameState().getValue() == NumberGameViewModel.GameState.FINISHED) return;

        if (abandonmentTimer != null) {
            abandonmentTimer.cancel();
            abandonmentTimer = null;
        }

        String expr = viewModel.submitResult();
        Double result = evaluateMathExpressionUseCase.evaluate(expr);
        long resLong = result != null ? Math.round(result) : 0L;
        int score = scoringService.calculateScore(expr, Integer.parseInt(viewModel.getTargetNumber().getValue()));

        if (sharedViewModel.isChallenge()) {
            writeStats(true, score);
            sharedViewModel.addCurrentPlayerPoints(score);
            sharedViewModel.advanceGamePhase();
            return;
        }

        if (matchId != null) {
            numberGameRepo.submitResult(matchId, gameKey, sharedViewModel.getIsPlayer1(), resLong);
            TextView tvWait = getView() != null ? getView().findViewById(R.id.tvWaitingOpponent) : null;
            if (tvWait != null) tvWait.setVisibility(View.VISIBLE);
            View btnConfirm = getView() != null ? getView().findViewById(R.id.btnConfirmCalc) : null;
            if (btnConfirm != null) btnConfirm.setEnabled(false);
            View btnClearAll = getView() != null ? getView().findViewById(R.id.btnClearAll) : null;
            if (btnClearAll != null) btnClearAll.setEnabled(false);
        } else {
            String targetStr = viewModel.getTargetNumber().getValue();
            long targetLong = targetStr != null && !targetStr.equals("---") ? Long.parseLong(targetStr) : 0;
            int points = scoringService.calculatePoints(targetLong, resLong, 0, isActivePlayer);

            writeStats(resLong == targetLong, points);

            sharedViewModel.stopTimer();
            sharedViewModel.addCurrentPlayerPoints(points);
            Toast.makeText(getContext(), getString(R.string.game_result_toast, resLong, points), Toast.LENGTH_LONG).show();
            sharedViewModel.advanceGamePhase();
        }
    }


    private void checkOpponentStatus() {
        if (matchId == null || opponentAbandoned) return;
        if (opponentUserId == null) loadOpponentUserId();

        if (abandonmentTimer != null) abandonmentTimer.cancel();
        abandonmentTimer = gameStateMonitor.startAbandonmentWatch(
                () -> opponentUserId,
                () -> {
                    if (!isAdded() || opponentAbandoned) return;
                    opponentAbandoned = true;
                    handleOpponentAbandonment();
                }
        );
    }


    private void loadOpponentUserId() {
        if (matchId == null) return;

        com.example.slagalica.data.MatchRepository matchRepo =
                new com.example.slagalica.data.MatchRepository();
        matchRepo.getMatch(matchId)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        boolean isP1 = sharedViewModel.getIsPlayer1();
                        opponentUserId = isP1 ?
                                doc.getString("player2_id") :
                                doc.getString("player1_id");
                    }
                });
    }


    private void handleOpponentAbandonment() {
        if (matchId != null && opponentUserId != null) {
            gameStateMonitor.detectAndHandleAbandonment(
                    matchId,
                    opponentUserId,
                    com.google.firebase.auth.FirebaseAuth.getInstance().getUid()
            ).addOnSuccessListener(wasAbandoned -> {
                if (Boolean.TRUE.equals(wasAbandoned)) {
                    Toast.makeText(getContext(), "Protivnik je napustio igru!", Toast.LENGTH_SHORT).show();
                    sharedViewModel.setOpponentAbandoned(true);
                    writeOpponentDummyResult();
                } else {
                    opponentAbandoned = false;
                    checkOpponentStatus();
                }
            });
        }
    }

    private void writeOpponentDummyResult() {
        if (matchId == null) return;
        boolean opponentIsP1 = !sharedViewModel.getIsPlayer1();
        numberGameRepo.submitResult(matchId, gameKey, opponentIsP1, 0L);
    }
}
