package com.example.rc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class OnlineChessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String sessionId = getIntent().getStringExtra("session_id");

        Intent chessIntent = new Intent(this, ChessGameActivity.class);
        chessIntent.putExtra("is_online_game", true);
        chessIntent.putExtra("session_id", sessionId);

        chessIntent.putExtra("player_color_white", true);
        chessIntent.putExtra("opponent_username", "Player2");
        chessIntent.putExtra("opponent_king_type", "dragon");

        chessIntent.putExtra("is_timed_game", true);
        chessIntent.putExtra("game_time_seconds", 600);

        startActivity(chessIntent);
        finish();
    }
}