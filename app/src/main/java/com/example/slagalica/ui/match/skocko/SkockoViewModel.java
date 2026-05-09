package com.example.slagalica.ui.match.skocko;

import androidx.lifecycle.ViewModel;
import java.util.Random;
public class SkockoViewModel extends ViewModel {

    public boolean rescueCalled = false;
    public final String[] symbols = {"☻", "♣", "♠", "♥", "▲", "★"};

    public String[] secretCombination = new String[4];
    public String[] currentInput = new String[4];

    public int currentAttempt = 0;
    public int currentInputIndex = 0;

    public int playerScore = 0;
    public int systemScore = 0;

    public boolean playerTurnFinished = false;
    public boolean opponentAttemptUsed = false;
    public boolean roundFinished = false;

    public SkockoViewModel() {
        generateSecretCombination();
    }

    public void generateSecretCombination() {
        Random random = new Random();

        for (int i = 0; i < 4; i++) {
            secretCombination[i] = symbols[random.nextInt(symbols.length)];
        }
    }

    public boolean addSymbol(String symbol) {
        if (currentInputIndex >= 4 || roundFinished) return false;

        currentInput[currentInputIndex] = symbol;
        currentInputIndex++;
        return true;
    }

    public boolean isInputFull() {
        return currentInputIndex == 4;
    }

    public void clearInput() {
        currentInput = new String[4];
        currentInputIndex = 0;
    }

    public int[] checkCurrentInput() {
        return checkCombination(currentInput);
    }

    public int[] checkCombination(String[] guess) {
        int correctPlace = 0;
        int wrongPlace = 0;

        boolean[] usedSecret = new boolean[4];
        boolean[] usedGuess = new boolean[4];

        for (int i = 0; i < 4; i++) {
            if (guess[i].equals(secretCombination[i])) {
                correctPlace++;
                usedSecret[i] = true;
                usedGuess[i] = true;
            }
        }

        for (int i = 0; i < 4; i++) {
            if (usedGuess[i]) continue;

            for (int j = 0; j < 4; j++) {
                if (!usedSecret[j] && guess[i].equals(secretCombination[j])) {
                    wrongPlace++;
                    usedSecret[j] = true;
                    break;
                }
            }
        }

        return new int[]{correctPlace, wrongPlace};
    }

    public boolean isSolved(int[] result) {
        return result[0] == 4;
    }

    public int calculatePlayerPoints() {
        if (currentAttempt == 0 || currentAttempt == 1) return 20;
        if (currentAttempt == 2 || currentAttempt == 3) return 15;
        return 10;
    }

    public String[] generateSystemGuess() {
        Random random = new Random();
        String[] guess = new String[4];

        for (int i = 0; i < 4; i++) {
            guess[i] = symbols[random.nextInt(symbols.length)];
        }

        return guess;
    }

    public boolean removeSymbolAt(int index) {
        if (index < 0 || index >= currentInputIndex) return false;

        for (int i = index; i < currentInputIndex - 1; i++) {
            currentInput[i] = currentInput[i + 1];
        }

        currentInput[currentInputIndex - 1] = null;
        currentInputIndex--;

        return true;
    }
}