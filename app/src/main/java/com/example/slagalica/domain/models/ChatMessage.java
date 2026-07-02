package com.example.slagalica.domain.models;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String senderName;
    private String senderId;
    private String text;
    private Timestamp timestamp;

    public ChatMessage() {}

    public ChatMessage(String senderName, String senderId, String text, Timestamp timestamp) {
        this.senderName = senderName;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
