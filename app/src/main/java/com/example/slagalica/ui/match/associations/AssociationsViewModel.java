package com.example.slagalica.ui.match.associations;

import androidx.lifecycle.ViewModel;

public class AssociationsViewModel extends ViewModel {
    public String[][] fields = new String[4][4];
    public String[] columnSolutions = new String[4];
    public String finalSolution = "";

    public boolean puzzleLoaded = false;

    public final boolean[][] openedFields = new boolean[4][4];
    public final boolean[] solvedColumns = new boolean[4];

    public int activeColumn = -1;
    public boolean freeGuessMode = false;

    public int currentScore = 0;
    public int currentPlayer = 1;

    public boolean finalSolved = false;


    public void loadPuzzle(String[][] loadedFields, String[] loadedColumnSolutions, String loadedFinalSolution) {
        fields = loadedFields;
        columnSolutions = loadedColumnSolutions;
        finalSolution = loadedFinalSolution;
        puzzleLoaded = true;
    }

    public boolean openField(int column, int row) {
        if (openedFields[column][row]) return false;
        if (solvedColumns[column]) return false;
        if (finalSolved) return false;

        openedFields[column][row] = true;
        activeColumn = column;
        freeGuessMode = false;

        return true;
    }

    public boolean checkColumnAnswer(int column, String answer) {
        return answer.trim().equalsIgnoreCase(columnSolutions[column]);
    }

    public boolean checkFinalAnswer(String answer) {
        return answer.trim().equalsIgnoreCase(finalSolution);
    }

    public int solveColumn(int column) {
        int points = calculateColumnPoints(column);

        solvedColumns[column] = true;
        currentScore += points;

        for (int row = 0; row < 4; row++) {
            openedFields[column][row] = true;
        }

        activeColumn = -1;
        freeGuessMode = true;

        return points;
    }

    public int solveFinal() {
        int points = calculateFinalPoints();

        finalSolved = true;
        currentScore += points;

        for (int column = 0; column < 4; column++) {
            solvedColumns[column] = true;

            for (int row = 0; row < 4; row++) {
                openedFields[column][row] = true;
            }
        }

        activeColumn = -1;
        freeGuessMode = false;

        return points;
    }

    public int calculateColumnPoints(int column) {
        return 2 + countUnopenedFields(column);
    }

    public int calculateFinalPoints() {
        int points = 7;

        for (int column = 0; column < 4; column++) {
            if (!isColumnOpened(column) && !solvedColumns[column]) {
                points += 6;
            } else if (!solvedColumns[column]) {
                points += 2 + countUnopenedFields(column);
            }
        }

        return points;
    }

    public int countUnopenedFields(int column) {
        int count = 0;

        for (int row = 0; row < 4; row++) {
            if (!openedFields[column][row]) {
                count++;
            }
        }

        return count;
    }

    public boolean isColumnOpened(int column) {
        for (int row = 0; row < 4; row++) {
            if (openedFields[column][row]) {
                return true;
            }
        }

        return false;
    }

    public void resetTurn() {
        activeColumn = -1;
        freeGuessMode = false;
    }
}
