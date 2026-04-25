package com.example.slagalica.domain.service;

public class StepByStepScoringService {

    public int calculatePoints(int currentRevealedStep, boolean isOpponentPhase) {
        if (isOpponentPhase) {
            return 5;
        }
        if (currentRevealedStep <= 1) return 20;
        int points = 20 - ((currentRevealedStep - 1) * 2);
        return Math.max(8, points);
    }

}
