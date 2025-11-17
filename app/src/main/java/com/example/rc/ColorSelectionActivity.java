package com.example.rc;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.example.rc.models.King;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ColorSelectionActivity extends AppCompatActivity {

    private static final String TAG = "ColorSelectionActivity";
    private static final int REQUEST_TIME_SELECTION = 1;
    private int selectedTimeMinutes = 10;

    private Spinner spinnerWhiteKing, spinnerBlackKing;
    private String selectedWhiteKingType = "human";
    private String selectedBlackKingType = "human";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_selection);
        Log.d(TAG, "onCreate started");

        initViews();
        setupKingSpinners();
        updateTimeDisplay();
        Log.d(TAG, "ColorSelectionActivity setup completed");
    }

    private void initViews() {
        Button btnBack = findViewById(R.id.btnBack);
        Button btnStartGame = findViewById(R.id.btnStartGame);
        Button btnTimeSettings = findViewById(R.id.btnTimeSettings);

        spinnerWhiteKing = findViewById(R.id.spinnerWhiteKing);
        spinnerBlackKing = findViewById(R.id.spinnerBlackKing);

        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Back to MainActivity");
            finish();
        });

        btnTimeSettings.setOnClickListener(v -> {
            Log.d(TAG, "Opening TimeSelection");
            openTimeSelection();
        });

        btnStartGame.setOnClickListener(v -> {
            Log.d(TAG, "Starting offline game");
            startOfflineGame();
        });
    }

    private void setupKingSpinners() {
        List<King> kings = Arrays.asList(
                new King(R.drawable.king_of_man_bg, "Король людей", "...", "Люди", "Дипломатия"),
                new King(R.drawable.king_of_dragon_bg, "Король драконов", "...", "Драконы", "Дыхание дракона"),
                new King(R.drawable.king_of_elf_bg, "Король эльфов", "...", "Эльфы", "Лесная магия"),
                new King(R.drawable.king_of_gnom_bg, "Король гномов", "...", "Гномы", "Подземные ходы")
        );

        ArrayAdapter<King> adapter = new ArrayAdapter<King>(this,
                android.R.layout.simple_spinner_item, kings) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(getItem(position).getName());
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setText(getItem(position).getName());
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWhiteKing.setAdapter(adapter);
        spinnerBlackKing.setAdapter(adapter);

        spinnerWhiteKing.setSelection(0);
        spinnerBlackKing.setSelection(0);
    }

    private void openTimeSelection() {
        Intent intent = new Intent(this, TimeSelectionActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("current_time_minutes", selectedTimeMinutes);
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_TIME_SELECTION);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_TIME_SELECTION && resultCode == RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                Bundle extras = data.getExtras();
                selectedTimeMinutes = extras.getInt("selected_time_minutes", 10);
                Log.d(TAG, "Time received from TimeSelection: " + selectedTimeMinutes);

                updateTimeDisplay();

                String timeDisplay = formatTimeForDisplay();
                Toast.makeText(this, "Время установлено: " + timeDisplay, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateTimeDisplay() {
        Button btnTimeSettings = findViewById(R.id.btnTimeSettings);
        if (btnTimeSettings != null) {
            String currentTime = formatTimeForDisplay();
            btnTimeSettings.setText("Время: " + currentTime);
            Log.d(TAG, "Time display updated: " + currentTime);
        }
    }

    private void startOfflineGame() {
        King whiteKing = (King) spinnerWhiteKing.getSelectedItem();
        King blackKing = (King) spinnerBlackKing.getSelectedItem();

        selectedWhiteKingType = getKingTypeFromName(whiteKing.getName());
        selectedBlackKingType = getKingTypeFromName(blackKing.getName());

        Log.d(TAG, "Starting offline game - White King: " + selectedWhiteKingType +
                ", Black King: " + selectedBlackKingType + ", time: " + selectedTimeMinutes + " minutes");

        Intent intent = new Intent(this, ChessGameActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean("is_online_game", false);
        bundle.putInt("game_time_minutes", selectedTimeMinutes);
        bundle.putInt("game_time_seconds", selectedTimeMinutes * 60);
        bundle.putBoolean("is_timed_game", selectedTimeMinutes > 0);
        bundle.putString("time_display", formatTimeForDisplay());

        bundle.putString("white_player_name", "Игрок 1");
        bundle.putString("black_player_name", "Игрок 2");
        bundle.putString("white_king_type", selectedWhiteKingType);
        bundle.putString("black_king_type", selectedBlackKingType);

        intent.putExtras(bundle);

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private String getKingTypeFromName(String kingName) {
        switch (kingName) {
            case "Король людей": return "human";
            case "Король драконов": return "dragon";
            case "Король эльфов": return "elf";
            case "Король гномов": return "gnome";
            default: return "human";
        }
    }

    private String formatTimeForDisplay() {
        if (selectedTimeMinutes == 0) {
            return "Без ограничения";
        } else if (selectedTimeMinutes == 1) {
            return "1 минута";
        } else {
            return selectedTimeMinutes + " минут";
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences preferences = newBase.getSharedPreferences("AppSettings", MODE_PRIVATE);
        String language = preferences.getString("language", "ru");
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Resources resources = newBase.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        Context context = newBase.createConfigurationContext(configuration);
        super.attachBaseContext(context);
    }
}