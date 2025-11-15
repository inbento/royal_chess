package com.example.rc.models;

import java.util.List;

public class GameSession {
    private String sessionId;
    private String player1Id;
    private String player2Id;
    private String player1Color;
    private String player2Color;
    private int timeMinutes;
    private String status; // "waiting", "matched", "playing", "finished"
    private long createdAt;
    private String currentFen;
    private List<String> moves;

    public String getSessionId() {
        return sessionId;
    }

    public GameSession(String sessionId) {
        this.sessionId = sessionId;
    }

}