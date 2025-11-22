package com.example.rc.chess;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class ChessTimer {
    private Handler handler;
    private Runnable timerRunnable;
    private long whiteTimeLeft;
    private long blackTimeLeft;
    private boolean isWhiteTurn;
    private TextView tvWhiteTimer, tvBlackTimer;
    private View indicatorWhite, indicatorBlack;
    private TimerListener listener;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private long lastUpdateTime;

    public interface TimerListener {
        void onTimeOut(boolean isWhite);
        void onTimeUpdate(long whiteTime, long blackTime);
    }

    public ChessTimer(long initialTimeMillis, TextView tvWhiteTimer, TextView tvBlackTimer,
                      View indicatorWhite, View indicatorBlack, TimerListener listener) {
        this.whiteTimeLeft = initialTimeMillis;
        this.blackTimeLeft = initialTimeMillis;
        this.tvWhiteTimer = tvWhiteTimer;
        this.tvBlackTimer = tvBlackTimer;
        this.indicatorWhite = indicatorWhite;
        this.indicatorBlack = indicatorBlack;
        this.listener = listener;
        this.isWhiteTurn = true;

        this.handler = new Handler(Looper.getMainLooper());

        initTimerRunnable();
        updateTimerDisplays();
        updateIndicators();
    }

    private void initTimerRunnable() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning || isPaused) return;

                long currentTime = System.currentTimeMillis();
                long elapsed = currentTime - lastUpdateTime;
                lastUpdateTime = currentTime;

                if (isWhiteTurn) {
                    if (whiteTimeLeft > 0) {
                        whiteTimeLeft = Math.max(0, whiteTimeLeft - elapsed);
                        updateTimerDisplays();

                        if (whiteTimeLeft <= 0) {
                            handleTimeOut(true);
                            return;
                        }
                    }
                } else {
                    if (blackTimeLeft > 0) {
                        blackTimeLeft = Math.max(0, blackTimeLeft - elapsed);
                        updateTimerDisplays();

                        if (blackTimeLeft <= 0) {
                            handleTimeOut(false);
                            return;
                        }
                    }
                }

                if (listener != null) {
                    listener.onTimeUpdate(whiteTimeLeft, blackTimeLeft);
                }

                // Планируем следующее обновление
                handler.postDelayed(this, 100);
            }
        };
    }

    public void start() {
        if (isRunning) return;

        isRunning = true;
        isPaused = false;
        lastUpdateTime = System.currentTimeMillis();

        handler.removeCallbacks(timerRunnable); // Убедимся что старый удален
        handler.post(timerRunnable);

        updateIndicators();

        Log.d("ChessTimer", "Timer started - White: " + whiteTimeLeft + "ms, Black: " + blackTimeLeft + "ms");
    }

    public void switchTurn() {
        if (!isRunning) {
            start();
            return;
        }

        // Останавливаем текущий таймер
        handler.removeCallbacks(timerRunnable);

        // Переключаем ход
        isWhiteTurn = !isWhiteTurn;
        lastUpdateTime = System.currentTimeMillis();

        // Запускаем снова
        handler.post(timerRunnable);

        updateIndicators();

        Log.d("ChessTimer", "Turn switched to: " + (isWhiteTurn ? "White" : "Black"));
    }

    private void handleTimeOut(boolean isWhite) {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);

        if (isWhite) {
            whiteTimeLeft = 0;
        } else {
            blackTimeLeft = 0;
        }

        updateTimerDisplays();
        updateIndicators();

        Log.d("ChessTimer", "Time out for: " + (isWhite ? "White" : "Black"));

        if (listener != null) {
            listener.onTimeOut(isWhite);
        }
    }

    public void pause() {
        if (!isRunning || isPaused) return;

        isPaused = true;
        handler.removeCallbacks(timerRunnable);

        Log.d("ChessTimer", "Timer paused");
    }

    public void resume() {
        if (!isRunning || !isPaused) return;

        isPaused = false;
        lastUpdateTime = System.currentTimeMillis();
        handler.post(timerRunnable);

        Log.d("ChessTimer", "Timer resumed");
    }

    public void stop() {
        isRunning = false;
        isPaused = false;
        handler.removeCallbacks(timerRunnable);

        Log.d("ChessTimer", "Timer stopped");
    }

    public void reset(long initialTimeMillis) {
        stop();
        whiteTimeLeft = initialTimeMillis;
        blackTimeLeft = initialTimeMillis;
        isWhiteTurn = true;
        isRunning = false;
        isPaused = false;
        updateTimerDisplays();
        updateIndicators();

        Log.d("ChessTimer", "Timer reset");
    }

    private void updateTimerDisplays() {
        if (tvWhiteTimer != null) {
            tvWhiteTimer.setText(formatTime(whiteTimeLeft));
        }
        if (tvBlackTimer != null) {
            tvBlackTimer.setText(formatTime(blackTimeLeft));
        }
    }

    private void updateIndicators() {
        if (indicatorWhite != null) {
            indicatorWhite.setVisibility(isWhiteTurn ? View.VISIBLE : View.INVISIBLE);
        }
        if (indicatorBlack != null) {
            indicatorBlack.setVisibility(!isWhiteTurn ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "0:00";

        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isWhiteTurn() {
        return isWhiteTurn;
    }

    public long getWhiteTimeLeft() {
        return whiteTimeLeft;
    }

    public long getBlackTimeLeft() {
        return blackTimeLeft;
    }
}