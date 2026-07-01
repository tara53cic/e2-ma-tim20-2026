package com.example.slagalica.domain.models;

public class FriendRequest {
    private String id;
    private String fromUid;
    private String fromUsername;
    private String toUid;
    private String status; // "PENDING", "ACCEPTED", "DECLINED", "CANCELLED"
    private long createdAt;
    private String matchId;

    public FriendRequest() {}

    public FriendRequest(String id, String fromUid, String fromUsername,
                         String toUid, String status, long createdAt) {
        this.id = id;
        this.fromUid = fromUid;
        this.fromUsername = fromUsername;
        this.toUid = toUid;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFromUid() { return fromUid; }
    public void setFromUid(String fromUid) { this.fromUid = fromUid; }
    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }
    public String getToUid() { return toUid; }
    public void setToUid(String toUid) { this.toUid = toUid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
}