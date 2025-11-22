package com.example.rc.models;

public class ChessMove {
    private String moveId;
    private String playerId;
    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;
    private int promotionType;
    private long timestamp;
    private String moveType;

    public ChessMove() {
    }

    public ChessMove(String playerId, int fromRow, int fromCol, int toRow, int toCol, int promotionType, String moveType) {
        this.playerId = playerId != null ? playerId : "unknown";
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.promotionType = promotionType;
        this.moveType = moveType != null ? moveType : "normal";
        this.timestamp = System.currentTimeMillis();
    }

    public String getMoveId() { return moveId; }
    public void setMoveId(String moveId) { this.moveId = moveId; }

    public String getPlayerId() {
        return playerId != null ? playerId : "unknown";
    }

    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public int getFromRow() { return fromRow; }
    public void setFromRow(int fromRow) { this.fromRow = fromRow; }

    public int getFromCol() { return fromCol; }
    public void setFromCol(int fromCol) { this.fromCol = fromCol; }

    public int getToRow() { return toRow; }
    public void setToRow(int toRow) { this.toRow = toRow; }

    public int getToCol() { return toCol; }
    public void setToCol(int toCol) { this.toCol = toCol; }

    public int getPromotionType() { return promotionType; }
    public void setPromotionType(int promotionType) { this.promotionType = promotionType; }

    public String getMoveType() {
        return moveType != null ? moveType : "normal";
    }
    public void setMoveType(String moveType) { this.moveType = moveType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
