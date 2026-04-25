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
import com.example.slagalica.domain.service.NumberGameScoringService;
import com.example.slagalica.domain.usecase.EvaluateMathExpressionUseCase;
import com.example.slagalica.ui.match.MatchViewModel;
import com.example.slagalica.utils.ShakeDetector;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class NumberGameFragment extends Fragment {

    private NumberGameViewModel viewModel;
    private MatchViewModel sharedViewModel;
    private EvaluateMathExpressionUseCase evaluateMathExpressionUseCase;
    private NumberGameScoringService scoringService;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

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

        TextView tvTargetNumber = view.findViewById(R.id.tvTargetNumber);
        TextView tvCalcScreen = view.findViewById(R.id.tvCalcScreen);
        MaterialButton btnStopTarget = view.findViewById(R.id.btnStopTarget);

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            shakeDetector = new ShakeDetector();
            shakeDetector.setOnShakeListener(() -> {
                NumberGameViewModel.GameState state = viewModel.getGameState().getValue();
                if (state == NumberGameViewModel.GameState.SHUFFLE_TARGET || state == NumberGameViewModel.GameState.SHUFFLE_NUMBERS) {
                    String phase = sharedViewModel.getCurrentFragment().getValue();
                    if (!"MOJ_BROJ_R2".equals(phase)) {
                        viewModel.onStopClicked();
                    }
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
            String currentPhase = sharedViewModel.getCurrentFragment().getValue();
            boolean isOpponentRound = "MOJ_BROJ_R2".equals(currentPhase);

            if (state == NumberGameViewModel.GameState.SHUFFLE_TARGET) {
                btnStopTarget.setVisibility(isOpponentRound ? View.GONE : View.VISIBLE);
                sharedViewModel.startRoundTimer(5, () -> viewModel.onStopClicked());
            } else if (state == NumberGameViewModel.GameState.PLAYING) {
                btnStopTarget.setVisibility(View.GONE);
                sharedViewModel.startRoundTimer(60, () -> submitGameResult());
            } else if (state == NumberGameViewModel.GameState.FINISHED) {
                btnStopTarget.setVisibility(View.GONE);
            }
        });

        viewModel.getCurrentNumberIndex().observe(getViewLifecycleOwner(), index -> {
            if (index >= 0 && viewModel.getGameState().getValue() == NumberGameViewModel.GameState.SHUFFLE_NUMBERS) {
                String currentPhase = sharedViewModel.getCurrentFragment().getValue();
                boolean isOpponentRound = "MOJ_BROJ_R2".equals(currentPhase);
                btnStopTarget.setVisibility(isOpponentRound ? View.GONE : View.VISIBLE);
                sharedViewModel.startRoundTimer(5, () -> viewModel.onStopClicked());
            }
        });

        viewModel.getUsedNumbers().observe(getViewLifecycleOwner(), usedList -> {
            for (int i = 0; i < numberButtons.size(); i++) {
                numberButtons.get(i).setEnabled(!usedList.get(i));
            }
        });

        viewModel.getCurrentExpression().observe(getViewLifecycleOwner(), tvCalcScreen::setText);

        btnStopTarget.setOnClickListener(v -> viewModel.onStopClicked());

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

        viewModel.startTargetShuffle();
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

    private void submitGameResult() {
        if (viewModel.getGameState().getValue() == NumberGameViewModel.GameState.FINISHED) return;

        sharedViewModel.stopTimer();
        String expr = viewModel.submitResult();
        Double result = evaluateMathExpressionUseCase.evaluate(expr);
        long resLong = result != null ? Math.round(result) : 0L;

        String targetStr = viewModel.getTargetNumber().getValue();
        long targetLong = targetStr != null && !targetStr.equals("---") ? Long.parseLong(targetStr) : 0;


        long opponentResult = 0L;

        int points = scoringService.calculatePoints(targetLong, resLong, opponentResult, true);

        sharedViewModel.addCurrentPlayerPoints(points);

        Toast.makeText(getContext(), "Result: " + resLong + " | Points: " + points, Toast.LENGTH_LONG).show();
        sharedViewModel.advanceGamePhase();
    }
}
