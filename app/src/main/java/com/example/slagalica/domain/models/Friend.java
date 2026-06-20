package com.example.slagalica.domain.models;

public class Friend {
    private String uid;
    private String username;
    private String email;
    private int stars;
    private int league;
    private int avatarColorIndex;
    private String status; // "online", "offline", "in_game"

    public Friend() {}

    public Friend(String uid, String username, String email, int stars, int league, int avatarColorIndex) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.stars = stars;
        this.league = league;
        this.avatarColorIndex = avatarColorIndex;
        this.status = "offline";
    }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}