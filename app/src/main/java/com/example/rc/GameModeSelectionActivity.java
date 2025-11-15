package com.example.rc;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class GameModeSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_mode_selection);

        Button btnSingleplayer = findViewById(R.id.btnSingleplayer);
        Button btnOnline = findViewById(R.id.btnOnline);
        Button btnBack = findViewById(R.id.btnBack);

        btnSingleplayer.setOnClickListener(v -> {
            Intent intent = new Intent(this, ColorSelectionActivity.class);
            startActivity(intent);
        });

        btnOnline.setOnClickListener(v -> {
            Intent intent = new Intent(this, OnlineLobbyActivity.class);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }
}