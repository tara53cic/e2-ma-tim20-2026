package com.example.slagalica.domain.service;

public class NumberGameScoringService {

    public int calculatePoints(long target, long myResult, long opponentResult, boolean isMyRound) {
        if (myResult == target) {
            return 10;
        }
        if (opponentResult == target) {
            return 0;
        }

        if (myResult == 0) {
            return 0;
        }

        long myDiff = Math.abs(target - myResult);
        long opDiff = (opponentResult == 0) ? Long.MAX_VALUE : Math.abs(target - opponentResult);

        if (myDiff < opDiff) {
            return 5;
        } else if (myDiff == opDiff) {
            return isMyRound ? 5 : 0;
        }

        return 0;
    }

}
