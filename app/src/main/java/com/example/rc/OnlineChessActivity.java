package com.example.rc;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class OnlineChessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_chess); // или любой другой layout

        String sessionId = getIntent().getStringExtra("session_id");
        Toast.makeText(this, "Онлайн игра началась! Session: " + sessionId, Toast.LENGTH_SHORT).show();

        // TODO: Реализовать онлайн-шахматы
    }
}