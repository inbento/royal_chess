package com.example.rc.models;

import android.util.Log;

public class GameStat {
    private int id;
    private int userId;
    private String result;
    private String color;
    private int duration;
    private String date;
    private int movesCount;

    public GameStat() {}

    public GameStat(int userId, String result, String color, int duration, int movesCount) {
        this.userId = userId;
        this.result = result;
        this.color = color;
        this.duration = duration;
        this.movesCount = movesCount;
        this.date = String.valueOf(System.currentTimeMillis());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public int getMovesCount() {
        return movesCount;
    }
    public void setMovesCount(int movesCount) {
        this.movesCount = movesCount;
    }
}