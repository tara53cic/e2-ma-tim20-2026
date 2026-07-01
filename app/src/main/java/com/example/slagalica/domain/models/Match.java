package com.example.slagalica.domain.models;

public class Match {
    private String id;
    private String player1_id;
    private String player2_id;
    private int player1_score;
    private int player2_score;
    private String status;
    private boolean friendly;
    private String abandonedBy;
    private long createdAt;
    private boolean player1_timedOut;
    private boolean player2_timedOut;

    public Match() {}

    public Match(String id, String player1_id, String player2_id,
                 int player1_score, int player2_score, String status) {
        this.id = id;
        this.player1_id = player1_id;
        this.player2_id = player2_id;
        this.player1_score = player1_score;
        this.player2_score = player2_score;
        this.status = status;
        this.friendly = false;
        this.createdAt = System.currentTimeMillis();
        this.player1_timedOut = false;
        this.player2_timedOut = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlayer1_id() { return player1_id; }
    public void setPlayer1_id(String player1_id) { this.player1_id = player1_id; }
    public String getPlayer2_id() { return player2_id; }
    public void setPlayer2_id(String player2_id) { this.player2_id = player2_id; }
    public int getPlayer1_score() { return player1_score; }
    public void setPlayer1_score(int player1_score) { this.player1_score = player1_score; }
    public int getPlayer2_score() { return player2_score; }
    public void setPlayer2_score(int player2_score) { this.player2_score = player2_score; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isFriendly() { return friendly; }
    public void setFriendly(boolean friendly) { this.friendly = friendly; }
    public String getAbandonedBy() { return abandonedBy; }
    public void setAbandonedBy(String abandonedBy) { this.abandonedBy = abandonedBy; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public boolean isPlayer1_timedOut() { return player1_timedOut; }
    public void setPlayer1_timedOut(boolean player1_timedOut) { this.player1_timedOut = player1_timedOut; }
    public boolean isPlayer2_timedOut() { return player2_timedOut; }
    public void setPlayer2_timedOut(boolean player2_timedOut) { this.player2_timedOut = player2_timedOut; }
}