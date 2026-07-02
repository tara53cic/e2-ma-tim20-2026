package com.example.slagalica.domain.models;

public class Friend {
    private String uid;
    private String username;
    private String email;
    private int stars;
    private int league;
    private int avatarColorIndex;
    private int monthlyRank;
    private boolean online;
    private boolean inGame;

    public Friend() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public int getLeague() { return league; }
    public void setLeague(int league) { this.league = league; }
    public int getAvatarColorIndex() { return avatarColorIndex; }
    public void setAvatarColorIndex(int avatarColorIndex) { this.avatarColorIndex = avatarColorIndex; }
    public int getMonthlyRank() { return monthlyRank; }
    public void setMonthlyRank(int monthlyRank) { this.monthlyRank = monthlyRank; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public boolean isInGame() { return inGame; }
    public void setInGame(boolean inGame) { this.inGame = inGame; }
    public boolean canPlay() { return online && !inGame; }
}