package com.example.slagalica.domain.service;

public class NumberGameScoringService {

    public int calculatePoints(long target, long myResult, long opponentResult, boolean isMyRound) {

        long myDiff = Math.abs(target - myResult);
        long opDiff = Math.abs(target - opponentResult);


        if (myDiff < opDiff) {
            return (myDiff==0)? 10 : 5;
        } else if (myDiff == opDiff) {
            if (myDiff==0) {
                return isMyRound ? 10 : 0;
            }
            return isMyRound ? 5 : 0;
        }

        return 0;
    }

}
