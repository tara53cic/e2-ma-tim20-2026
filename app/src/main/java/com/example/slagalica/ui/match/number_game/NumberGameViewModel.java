package com.example.slagalica.ui.match.number_game;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.example.slagalica.domain.usecase.EvaluateMathExpressionUseCase;

public class NumberGameViewModel extends ViewModel {

    public enum GameState {
        IDLE, SHUFFLE_TARGET, SHUFFLE_NUMBERS, PLAYING, FINISHED
    }

    private final MutableLiveData<GameState> gameState = new MutableLiveData<>(GameState.IDLE);
    private final MutableLiveData<Integer> currentNumberIndex = new MutableLiveData<>(-1);
    private final MutableLiveData<String> targetNumber = new MutableLiveData<>("---");
    private final MutableLiveData<List<String>> smallNumbers = new MutableLiveData<>(Arrays.asList("","","","","",""));
    private final MutableLiveData<String> currentExpression = new MutableLiveData<>("");
    private final MutableLiveData<List<Boolean>> usedNumbers = new MutableLiveData<>(Arrays.asList(false,false,false,false,false,false));

    private final List<String> expressionTokens = new ArrayList<>();
    private final List<Integer> tokenSourceIndex = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final EvaluateMathExpressionUseCase mathUseCase = new EvaluateMathExpressionUseCase();

    public LiveData<GameState> getGameState() { return gameState; }
    public LiveData<Integer> getCurrentNumberIndex() { return currentNumberIndex; }
    public LiveData<String> getTargetNumber() { return targetNumber; }
    public LiveData<List<String>> getSmallNumbers() { return smallNumbers; }
    public LiveData<String> getCurrentExpression() { return currentExpression; }
    public LiveData<List<Boolean>> getUsedNumbers() { return usedNumbers; }

    private final int[] MID_POOL = {10, 15, 20};
    private final int[] LARGE_POOL = {25, 50, 75, 100};

    private Runnable shuffleNumberTask;
    private int shuffleIndex = 0;

    private Runnable shuffleTargetTask = new Runnable() {
        @Override
        public void run() {
            targetNumber.setValue(String.valueOf(random.nextInt(999) + 1));
            handler.postDelayed(this, 50);
        }
    };

    private Runnable createShuffleNumberTask(int index) {
        return new Runnable() {
            @Override
            public void run() {
                List<String> nums = new ArrayList<>(smallNumbers.getValue());
                String val;
                if (index < 4) val = String.valueOf(random.nextInt(9) + 1);
                else if (index == 4) val = String.valueOf(MID_POOL[random.nextInt(MID_POOL.length)]);
                else val = String.valueOf(LARGE_POOL[random.nextInt(LARGE_POOL.length)]);
                nums.set(index, val);
                smallNumbers.setValue(nums);
                handler.postDelayed(this, 50);
            }
        };
    }

    public void startTargetShuffle() {
        gameState.setValue(GameState.SHUFFLE_TARGET);
        handler.post(shuffleTargetTask);
    }

    private void startNextNumberShuffle() {
        if (shuffleIndex < 6) {
            gameState.setValue(GameState.SHUFFLE_NUMBERS);
            currentNumberIndex.setValue(shuffleIndex);
            shuffleNumberTask = createShuffleNumberTask(shuffleIndex);
            handler.post(shuffleNumberTask);
        } else {
            gameState.setValue(GameState.PLAYING);
            currentNumberIndex.setValue(-1);
        }
    }

    public void onStopClicked() {
        GameState state = gameState.getValue();
        if (state == GameState.SHUFFLE_TARGET) {
            handler.removeCallbacks(shuffleTargetTask);
            shuffleIndex = 0;
            startNextNumberShuffle();
        } else if (state == GameState.SHUFFLE_NUMBERS) {
            handler.removeCallbacks(shuffleNumberTask);
            shuffleIndex++;
            startNextNumberShuffle();
        }
    }

    public void onNumberClicked(int index, String number) {
        if (gameState.getValue() != GameState.PLAYING) return;

        if (!mathUseCase.canAppendNumber(expressionTokens)) {
            return;
        }

        List<Boolean> used = new ArrayList<>(usedNumbers.getValue());
        if (!used.get(index)) {
            used.set(index, true);
            usedNumbers.setValue(used);
            expressionTokens.add(number);
            tokenSourceIndex.add(index);
            updateExpression();
        }
    }

    public void onOperatorClicked(String op) {
        if (gameState.getValue() != GameState.PLAYING) return;

        if (!mathUseCase.canAppendOperator(expressionTokens, op)) {
            return;
        }

        expressionTokens.add(op);
        tokenSourceIndex.add(-1);
        updateExpression();
    }

    public void onBackspace() {
        if (gameState.getValue() != GameState.PLAYING) return;
        if (!expressionTokens.isEmpty()) {
            int lastIdx = expressionTokens.size() - 1;
            expressionTokens.remove(lastIdx);
            int source = tokenSourceIndex.remove(lastIdx);
            if (source >= 0) {
                List<Boolean> used = new ArrayList<>(usedNumbers.getValue());
                used.set(source, false);
                usedNumbers.setValue(used);
            }
            updateExpression();
        }
    }

    public void onClearAll() {
        if (gameState.getValue() != GameState.PLAYING) return;
        expressionTokens.clear();
        tokenSourceIndex.clear();
        usedNumbers.setValue(Arrays.asList(false,false,false,false,false,false));
        updateExpression();
    }

    private void updateExpression() {
        StringBuilder sb = new StringBuilder();
        for (String t : expressionTokens) sb.append(t).append(" ");
        currentExpression.setValue(sb.toString().trim());
    }

    public String submitResult() {
        gameState.setValue(GameState.FINISHED);
        return currentExpression.getValue();
    }
}
