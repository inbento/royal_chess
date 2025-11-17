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

    private String player1Username;
    private String player2Username;
    private String player1KingType;
    private String player2KingType;

    public GameSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() { return sessionId; }
    public String getPlayer1Id() { return player1Id; }
    public String getPlayer2Id() { return player2Id; }
    public String getPlayer1Color() { return player1Color; }
    public String getPlayer2Color() { return player2Color; }
    public int getTimeMinutes() { return timeMinutes; }
    public String getPlayer1Username() { return player1Username; }
    public String getPlayer2Username() { return player2Username; }
    public String getPlayer1KingType() { return player1KingType; }
    public String getPlayer2KingType() { return player2KingType; }

    public void setPlayer1Id(String player1Id) { this.player1Id = player1Id; }
    public void setPlayer2Id(String player2Id) { this.player2Id = player2Id; }
    public void setPlayer1Color(String player1Color) { this.player1Color = player1Color; }
    public void setPlayer2Color(String player2Color) { this.player2Color = player2Color; }
    public void setTimeMinutes(int timeMinutes) { this.timeMinutes = timeMinutes; }
    public void setPlayer1Username(String player1Username) { this.player1Username = player1Username; }
    public void setPlayer2Username(String player2Username) { this.player2Username = player2Username; }
    public void setPlayer1KingType(String player1KingType) { this.player1KingType = player1KingType; }
    public void setPlayer2KingType(String player2KingType) { this.player2KingType = player2KingType; }

    public String getOpponentUsername(String currentUserId) {
        if (currentUserId == null) {
            return "Соперник";
        }

        if (currentUserId.equals(player1Id) && player2Username != null) {
            return player2Username;
        } else if (currentUserId.equals(player2Id) && player1Username != null) {
            return player1Username;
        }
        return "Соперник";
    }

    public String getOpponentKingType(String currentUserId) {
        if (currentUserId == null) {
            return "human";
        }

        if (currentUserId.equals(player1Id) && player2KingType != null) {
            return player2KingType;
        } else if (currentUserId.equals(player2Id) && player1KingType != null) {
            return player1KingType;
        }
        return "human";
    }

    public boolean isPlayerWhite(String currentUserId) {
        if (currentUserId == null) {
            return true;
        }

        if (currentUserId.equals(player1Id)) {
            return "white".equals(player1Color);
        } else if (currentUserId.equals(player2Id)) {
            return "white".equals(player2Color);
        }
        return true;
    }

    public String getOpponentUsername() {
        return player2Username != null ? player2Username : "Соперник";
    }

    public String getOpponentKingType() {
        return player2KingType != null ? player2KingType : "human";
    }

    public boolean isPlayerWhite() {
        return "white".equals(player1Color);
    }
}