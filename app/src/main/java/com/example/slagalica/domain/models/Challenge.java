package com.example.slagalica.domain.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Challenge {
    private String id;
    private String challengerId;
    private String challengerName;
    private int bidStars;
    private int bidTokens;
    private List<String> playerIds = new ArrayList<>();
    private Map<String, String> playerNames = new HashMap<>();
    private Map<String, Integer> playerScores = new HashMap<>();
    private Map<String, Boolean> playersFinished = new HashMap<>();
    private String status; // OPEN, IN_PROGRESS, FINISHED
    private Timestamp createdAt;

    public Challenge() {}

    public Challenge(String id, String challengerId, String challengerName, int bidStars, int bidTokens) {
        this.id = id;
        this.challengerId = challengerId;
        this.challengerName = challengerName;
        this.bidStars = bidStars;
        this.bidTokens = bidTokens;
        this.playerIds.add(challengerId);
        this.playerNames.put(challengerId, challengerName);
        this.status = "OPEN";
        this.createdAt = Timestamp.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getChallengerId() { return challengerId; }
    public void setChallengerId(String challengerId) { this.challengerId = challengerId; }
    public String getChallengerName() { return challengerName; }
    public void setChallengerName(String challengerName) { this.challengerName = challengerName; }
    public int getBidStars() { return bidStars; }
    public void setBidStars(int bidStars) { this.bidStars = bidStars; }
    public int getBidTokens() { return bidTokens; }
    public void setBidTokens(int bidTokens) { this.bidTokens = bidTokens; }
    public List<String> getPlayerIds() { return playerIds; }
    public void setPlayerIds(List<String> playerIds) { this.playerIds = playerIds; }
    public Map<String, String> getPlayerNames() { return playerNames; }
    public void setPlayerNames(Map<String, String> playerNames) { this.playerNames = playerNames; }
    public Map<String, Integer> getPlayerScores() { return playerScores; }
    public void setPlayerScores(Map<String, Integer> playerScores) { this.playerScores = playerScores; }
    public Map<String, Boolean> getPlayersFinished() { return playersFinished; }
    public void setPlayersFinished(Map<String, Boolean> playersFinished) { this.playersFinished = playersFinished; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
