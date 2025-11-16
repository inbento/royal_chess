package com.example.rc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rc.database.DatabaseHelper;
import com.example.rc.models.User;
import com.example.rc.models.King;
import com.example.rc.models.GameSession;
import com.example.rc.OnlineGameManager.MatchmakingListener;

import java.util.Arrays;
import java.util.Random;

public class OnlineLobbyActivity extends AppCompatActivity {

    private RadioGroup radioColorGroup, radioTimeGroup;
    private Spinner spinnerKing;
    private Button btnFindMatch;
    private TextView tvSearchStatus, tvQueueInfo;
    private ProgressBar progressSearch;

    private OnlineGameManager gameManager;
    private User currentUser;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_lobby);

        dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("currentUserId", -1);
        currentUser = dbHelper.getUser(userId);
        gameManager = OnlineGameManager.getInstance();

        initViews();
        setupKingSpinner();
        setupButtonListeners();
    }

    private void initViews() {
        radioColorGroup = findViewById(R.id.radioColorGroup);
        radioTimeGroup = findViewById(R.id.radioTimeGroup);
        spinnerKing = findViewById(R.id.spinnerKing);
        btnFindMatch = findViewById(R.id.btnFindMatch);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);
        tvQueueInfo = findViewById(R.id.tvQueueInfo);
        progressSearch = findViewById(R.id.progressSearch);
    }

    private void setupKingSpinner() {
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
        spinnerKing.setAdapter(adapter);
    }

    private void setupButtonListeners() {
        btnFindMatch.setOnClickListener(v -> startMatchmaking());
    }

    private void startMatchmaking() {
        if (currentUser == null) {
            Toast.makeText(this, "Войдите в аккаунт для онлайн игры", Toast.LENGTH_SHORT).show();
            return;
        }

        String color = getSelectedColor();
        int timeMinutes = getSelectedTime();
        King selectedKing = (King) spinnerKing.getSelectedItem();

        String matchmakingId = generateMatchmakingId(currentUser);

        btnFindMatch.setEnabled(false);
        tvSearchStatus.setVisibility(View.VISIBLE);
        progressSearch.setVisibility(View.VISIBLE);
        tvSearchStatus.setText("🔍 Поиск противника...");

        gameManager.findMatch(currentUser, matchmakingId, color, timeMinutes, selectedKing,
                new MatchmakingListener() {
                    @Override
                    public void onMatchFound(GameSession session) {
                        runOnUiThread(() -> {
                            Intent intent = new Intent(OnlineLobbyActivity.this, OnlineChessActivity.class);
                            intent.putExtra("session_id", session.getSessionId());
                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onMatchmakingUpdate(int playersInQueue) {
                        runOnUiThread(() -> {
                            tvQueueInfo.setText("Игроков в очереди: " + playersInQueue);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(OnlineLobbyActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                            resetSearchUI();
                        });
                    }
                });
    }

    private String generateMatchmakingId(User user) {
        String onlineId = dbHelper.getUserOnlineId(user.getId());
        return "match_" + onlineId + "_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
    }

    private String getSelectedColor() {
        int selectedId = radioColorGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radioWhite) return "white";
        if (selectedId == R.id.radioBlack) return "black";
        return "random";
    }

    private int getSelectedTime() {
        int selectedId = radioTimeGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radio5min) return 5;
        if (selectedId == R.id.radio10min) return 10;
        if (selectedId == R.id.radio30min) return 30;
        return -1;
    }

    private void resetSearchUI() {
        btnFindMatch.setEnabled(true);
        tvSearchStatus.setVisibility(View.GONE);
        progressSearch.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameManager != null) {
            gameManager.cancelMatchmaking(currentUser);
        }
    }
}