package com.example.slagalica.domain.models;
public class UserGameStats {

    // Ko zna zna
    private int kzzTotalQuestions = 0;
    private int kzzCorrect = 0;
    private int kzzIncorrect = 0;
    private int kzzTotalPoints = 0;
    private int kzzGames = 0;

    // Spojnice
    private int spojniceTotalPairs = 0;
    private int spojniceConnected = 0;
    private int spojniceTotalPoints = 0;
    private int spojniceGames = 0;

    // Asocijacije
    private int asocijacijeTotal = 0;
    private int asocijacijeSolved = 0;
    private int asocijacijeTotalPoints = 0;
    private int asocijacijeGames = 0;

    // Skočko
    private int skockoGames = 0;
    private int skockoSolvedAt0 = 0;
    private int skockoSolvedAt1 = 0;
    private int skockoSolvedAt2 = 0;
    private int skockoSolvedAt3 = 0;
    private int skockoSolvedAt4 = 0;
    private int skockoSolvedAt5 = 0;
    private int skockoTotalPoints = 0;

    // Korak po korak
    private int kpkGames = 0;
    private int kpkSolvedAt0 = 0;
    private int kpkSolvedAt1 = 0;
    private int kpkSolvedAt2 = 0;
    private int kpkSolvedAt3 = 0;
    private int kpkSolvedAt4 = 0;
    private int kpkSolvedAt5 = 0;
    private int kpkSolvedAt6 = 0;
    private int kpkTotalPoints = 0;

    // Moj broj
    private int mojBrojGames = 0;
    private int mojBrojExact = 0;
    private int mojBrojTotalPoints = 0;

    // Opšta statistika
    private int totalMatches = 0;
    private int wins = 0;
    private int losses = 0;

    public UserGameStats() {}

    // Ko zna zna
    public int getKzzTotalQuestions() { return kzzTotalQuestions; }
    public void setKzzTotalQuestions(int v) { kzzTotalQuestions = v; }

    public int getKzzCorrect() { return kzzCorrect; }
    public void setKzzCorrect(int v) { kzzCorrect = v; }

    public int getKzzIncorrect() { return kzzIncorrect; }
    public void setKzzIncorrect(int v) { kzzIncorrect = v; }

    public int getKzzTotalPoints() { return kzzTotalPoints; }
    public void setKzzTotalPoints(int v) { kzzTotalPoints = v; }

    public int getKzzGames() { return kzzGames; }
    public void setKzzGames(int v) { kzzGames = v; }

    // Spojnice
    public int getSpojniceTotalPairs() { return spojniceTotalPairs; }
    public void setSpojniceTotalPairs(int v) { spojniceTotalPairs = v; }

    public int getSpojniceConnected() { return spojniceConnected; }
    public void setSpojniceConnected(int v) { spojniceConnected = v; }

    public int getSpojniceTotalPoints() { return spojniceTotalPoints; }
    public void setSpojniceTotalPoints(int v) { spojniceTotalPoints = v; }

    public int getSpojniceGames() { return spojniceGames; }
    public void setSpojniceGames(int v) { spojniceGames = v; }

    // Asocijacije
    public int getAsocijacijeTotal() { return asocijacijeTotal; }
    public void setAsocijacijeTotal(int v) { asocijacijeTotal = v; }

    public int getAsocijacijeSolved() { return asocijacijeSolved; }
    public void setAsocijacijeSolved(int v) { asocijacijeSolved = v; }

    public int getAsocijacijeTotalPoints() { return asocijacijeTotalPoints; }
    public void setAsocijacijeTotalPoints(int v) { asocijacijeTotalPoints = v; }

    public int getAsocijacijeGames() { return asocijacijeGames; }
    public void setAsocijacijeGames(int v) { asocijacijeGames = v; }

    // Skočko
    public int getSkockoGames() { return skockoGames; }
    public void setSkockoGames(int v) { skockoGames = v; }

    public int getSkockoSolvedAt0() { return skockoSolvedAt0; }
    public void setSkockoSolvedAt0(int v) { skockoSolvedAt0 = v; }

    public int getSkockoSolvedAt1() { return skockoSolvedAt1; }
    public void setSkockoSolvedAt1(int v) { skockoSolvedAt1 = v; }

    public int getSkockoSolvedAt2() { return skockoSolvedAt2; }
    public void setSkockoSolvedAt2(int v) { skockoSolvedAt2 = v; }

    public int getSkockoSolvedAt3() { return skockoSolvedAt3; }
    public void setSkockoSolvedAt3(int v) { skockoSolvedAt3 = v; }

    public int getSkockoSolvedAt4() { return skockoSolvedAt4; }
    public void setSkockoSolvedAt4(int v) { skockoSolvedAt4 = v; }

    public int getSkockoSolvedAt5() { return skockoSolvedAt5; }
    public void setSkockoSolvedAt5(int v) { skockoSolvedAt5 = v; }

    public int getSkockoTotalPoints() { return skockoTotalPoints; }
    public void setSkockoTotalPoints(int v) { skockoTotalPoints = v; }

    // Korak po korak
    public int getKpkGames() { return kpkGames; }
    public void setKpkGames(int v) { kpkGames = v; }

    public int getKpkSolvedAt0() { return kpkSolvedAt0; }
    public void setKpkSolvedAt0(int v) { kpkSolvedAt0 = v; }

    public int getKpkSolvedAt1() { return kpkSolvedAt1; }
    public void setKpkSolvedAt1(int v) { kpkSolvedAt1 = v; }

    public int getKpkSolvedAt2() { return kpkSolvedAt2; }
    public void setKpkSolvedAt2(int v) { kpkSolvedAt2 = v; }

    public int getKpkSolvedAt3() { return kpkSolvedAt3; }
    public void setKpkSolvedAt3(int v) { kpkSolvedAt3 = v; }

    public int getKpkSolvedAt4() { return kpkSolvedAt4; }
    public void setKpkSolvedAt4(int v) { kpkSolvedAt4 = v; }

    public int getKpkSolvedAt5() { return kpkSolvedAt5; }
    public void setKpkSolvedAt5(int v) { kpkSolvedAt5 = v; }

    public int getKpkSolvedAt6() { return kpkSolvedAt6; }
    public void setKpkSolvedAt6(int v) { kpkSolvedAt6 = v; }

    public int getKpkTotalPoints() { return kpkTotalPoints; }
    public void setKpkTotalPoints(int v) { kpkTotalPoints = v; }

    // Moj broj
    public int getMojBrojGames() { return mojBrojGames; }
    public void setMojBrojGames(int v) { mojBrojGames = v; }

    public int getMojBrojExact() { return mojBrojExact; }
    public void setMojBrojExact(int v) { mojBrojExact = v; }

    public int getMojBrojTotalPoints() { return mojBrojTotalPoints; }
    public void setMojBrojTotalPoints(int v) { mojBrojTotalPoints = v; }

    // Opšta statistika
    public int getTotalMatches() { return totalMatches; }
    public void setTotalMatches(int v) { totalMatches = v; }

    public int getWins() { return wins; }
    public void setWins(int v) { wins = v; }

    public int getLosses() { return losses; }
    public void setLosses(int v) { losses = v; }
}