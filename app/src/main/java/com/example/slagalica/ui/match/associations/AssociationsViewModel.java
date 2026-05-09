package com.example.slagalica.ui.match.associations;

import androidx.lifecycle.ViewModel;

public class AssociationsViewModel extends ViewModel {

    public final String[][] fields = {
            {"KRUNA", "DVOR", "PRESTO", "VLADAR"},
            {"DRUŠTVO", "KARTA", "POKER", "RULET"},
            {"KLJUČ", "KATANAC", "SEF", "LOZINKA"},
            {"BUBANJ", "GITARA", "MIKROFON", "BINA"}
    };

    public final String[] columnSolutions = {
            "KRALJ",
            "IGRA",
            "TAJNA",
            "MUZIKA"
    };

    public final String finalSolution = "ZABAVA";

    public final boolean[][] openedFields = new boolean[4][4];
    public final boolean[] solvedColumns = new boolean[4];

    public int activeColumn = -1;
    public int currentScore = 0;
    public int currentPlayer = 1;
    public int player1Score = 0;
    public int player2Score = 0;

    public void addPointsToCurrentPlayer(int points) {
        if (currentPlayer == 1) {
            player1Score += points;
        } else {
            player2Score += points;
        }
    }

    public void switchPlayer() {
        currentPlayer = currentPlayer == 1 ? 2 : 1;
        activeColumn = -1;
    }

    public boolean finalSolved = false;

    public boolean openField(int column, int row) {
        if (openedFields[column][row]) return false;
        if (solvedColumns[column]) return false;
        if (finalSolved) return false;

        openedFields[column][row] = true;
        activeColumn = column;
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
        addPointsToCurrentPlayer(points);
        currentScore += points;

        for (int row = 0; row < 4; row++) {
            openedFields[column][row] = true;
        }

        return points;
    }

    public int solveFinal() {
        int points = calculateFinalPoints();

        finalSolved = true;
        addPointsToCurrentPlayer(points);
        currentScore += points;

        for (int column = 0; column < 4; column++) {
            solvedColumns[column] = true;

            for (int row = 0; row < 4; row++) {
                openedFields[column][row] = true;
            }
        }

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
    }
}
