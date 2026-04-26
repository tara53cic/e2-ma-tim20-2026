package com.example.slagalica.domain.models;

public class Match {
    private String id;
    private String player1_id;
    private String player2_id;
    private int player1_score;
    private int player2_score;
    private String status;

    public Match() {
    }

    public Match(String id, String player1_id, String player2_id, int player1_score, int player2_score, String status) {
        this.id = id;
        this.player1_id = player1_id;
        this.player2_id = player2_id;
        this.player1_score = player1_score;
        this.player2_score = player2_score;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlayer1_id() {
        return player1_id;
    }

    public void setPlayer1_id(String player1_id) {
        this.player1_id = player1_id;
    }

    public String getPlayer2_id() {
        return player2_id;
    }

    public void setPlayer2_id(String player2_id) {
        this.player2_id = player2_id;
    }

    public int getPlayer1_score() {
        return player1_score;
    }

    public void setPlayer1_score(int player1_score) {
        this.player1_score = player1_score;
    }

    public int getPlayer2_score() {
        return player2_score;
    }

    public void setPlayer2_score(int player2_score) {
        this.player2_score = player2_score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

