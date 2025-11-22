package com.example.rc.models;

public class GameSession {
    private String sessionId;
    private String player1Id;
    private String player2Id;
    private String player1Username;
    private String player2Username;
    private String player1KingType;
    private String player2KingType;
    private String player1Color;
    private String player2Color;
    private int timeMinutes;
    private String status;
    private long createdAt;
    private String currentFen;
    private boolean isWhiteTurn;


    public GameSession() {
        this.createdAt = System.currentTimeMillis();
        this.status = "active";
        this.currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        this.isWhiteTurn = true;
    }

    public GameSession(String sessionId) {
        this();
        this.sessionId = sessionId;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getPlayer1Id() { return player1Id; }
    public void setPlayer1Id(String player1Id) { this.player1Id = player1Id; }

    public String getPlayer2Id() { return player2Id; }
    public void setPlayer2Id(String player2Id) { this.player2Id = player2Id; }

    public String getPlayer1Username() { return player1Username; }
    public void setPlayer1Username(String player1Username) { this.player1Username = player1Username; }

    public String getPlayer2Username() { return player2Username; }
    public void setPlayer2Username(String player2Username) { this.player2Username = player2Username; }

    public String getPlayer1KingType() { return player1KingType; }
    public void setPlayer1KingType(String player1KingType) { this.player1KingType = player1KingType; }

    public String getPlayer2KingType() { return player2KingType; }
    public void setPlayer2KingType(String player2KingType) { this.player2KingType = player2KingType; }

    public String getPlayer1Color() { return player1Color; }
    public void setPlayer1Color(String player1Color) { this.player1Color = player1Color; }

    public String getPlayer2Color() { return player2Color; }
    public void setPlayer2Color(String player2Color) { this.player2Color = player2Color; }

    public int getTimeMinutes() { return timeMinutes; }
    public void setTimeMinutes(int timeMinutes) { this.timeMinutes = timeMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getCurrentFen() { return currentFen; }
    public void setCurrentFen(String currentFen) { this.currentFen = currentFen; }

    public boolean isWhiteTurn() { return isWhiteTurn; }
    public void setWhiteTurn(boolean whiteTurn) { isWhiteTurn = whiteTurn; }

    public boolean isPlayerWhite(String playerId) {
        if (playerId == null) return false;
        if (playerId.equals(player1Id)) {
            return "white".equals(player1Color);
        } else if (playerId.equals(player2Id)) {
            return "white".equals(player2Color);
        }
        return false;
    }

    public String getOpponentUsername(String currentPlayerId) {
        if (currentPlayerId == null) return "Opponent";
        if (currentPlayerId.equals(player1Id)) {
            return player2Username != null ? player2Username : "Opponent";
        } else if (currentPlayerId.equals(player2Id)) {
            return player1Username != null ? player1Username : "Opponent";
        }
        return "Opponent";
    }

    public String getOpponentKingType(String currentPlayerId) {
        if (currentPlayerId == null) return "human";
        if (currentPlayerId.equals(player1Id)) {
            return player2KingType != null ? player2KingType : "human";
        } else if (currentPlayerId.equals(player2Id)) {
            return player1KingType != null ? player1KingType : "human";
        }
        return "human";
    }
}