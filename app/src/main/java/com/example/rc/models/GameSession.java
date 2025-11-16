package com.example.rc.models;

import java.util.List;
import java.util.ArrayList;

public class GameSession {
    private String sessionId;
    private String player1Id;
    private String player2Id;
    private String player1Color;
    private String player2Color;
    private int timeMinutes;
    private String status;
    private long createdAt;
    private String currentFen;
    private List<String> moves;

    public GameSession(String sessionId) {
        this.sessionId = sessionId;
        this.moves = new ArrayList<>();
        this.status = "waiting";
        this.createdAt = System.currentTimeMillis();
    }

    public String getSessionId() { return sessionId; }
    public String getPlayer1Id() { return player1Id; }
    public String getPlayer2Id() { return player2Id; }
    public String getPlayer1Color() { return player1Color; }
    public String getPlayer2Color() { return player2Color; }
    public int getTimeMinutes() { return timeMinutes; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public String getCurrentFen() { return currentFen; }
    public List<String> getMoves() { return moves; }

    public void setPlayer1Id(String player1Id) { this.player1Id = player1Id; }
    public void setPlayer2Id(String player2Id) { this.player2Id = player2Id; }
    public void setPlayer1Color(String player1Color) { this.player1Color = player1Color; }
    public void setPlayer2Color(String player2Color) { this.player2Color = player2Color; }
    public void setTimeMinutes(int timeMinutes) { this.timeMinutes = timeMinutes; }
    public void setStatus(String status) { this.status = status; }
    public void setCurrentFen(String currentFen) { this.currentFen = currentFen; }
    public void setMoves(List<String> moves) { this.moves = moves; }

    public void addMove(String move) {
        if (moves == null) moves = new ArrayList<>();
        moves.add(move);
    }
}