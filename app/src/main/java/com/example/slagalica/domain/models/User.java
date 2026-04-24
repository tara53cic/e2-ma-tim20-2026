package com.example.slagalica.domain.models;

public class User {
    private String email;
    private String username;
    private String region;
    private int tokens;
    private int stars;
    private int league;

    public User() {
    }

    public User(String email, String username, String region) {
        this.email = email;
        this.username = username;
        this.region = region;
        this.tokens = 5;
        this.stars = 0;
        this.league = 0;
    }

    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getRegion() { return region; }
    public int getTokens() { return tokens; }
    public int getStars() { return stars; }
    public int getLeague() { return league; }


    public void setEmail(String email) { this.email = email; }
    public void setUsername(String username) { this.username = username; }
    public void setRegion(String region) { this.region = region; }
    public void setTokens(int tokens) { this.tokens = tokens; }
    public void setStars(int stars) { this.stars = stars; }

    public void setLeague(int league) { this.league = league; }


}

