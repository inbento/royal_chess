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
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Locale;

public class TimeSelectionActivity extends AppCompatActivity {

    private static final String TAG = "TimeSelectionActivity";
    private int selectedTimeMinutes = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_selection);
        Log.d(TAG, "onCreate started");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            selectedTimeMinutes = extras.getInt("current_time_minutes", 10);
            Log.d(TAG, "Received time from intent: " + selectedTimeMinutes);
        }

        initViews();
        setupListeners();
        Log.d(TAG, "Activity setup completed");
    }

    private void initViews() {
        Log.d(TAG, "Initializing views");

        try {
            RadioGroup radioTimeGroup = findViewById(R.id.radioTimeGroup);
            Button btnConfirm = findViewById(R.id.btnConfirm);
            Button btnBack = findViewById(R.id.btnBack);

            if (radioTimeGroup != null) {
                setInitialSelection(radioTimeGroup);
            }

            if (radioTimeGroup != null) {
                radioTimeGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    handleTimeSelection(checkedId);
                });
            }

            if (btnConfirm != null) {
                btnConfirm.setOnClickListener(v -> {
                    Log.d(TAG, "Confirm button clicked, time: " + selectedTimeMinutes);
                    returnToColorSelection();
                });
            }

            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    Log.d(TAG, "Back button clicked");
                    finish();
                });
            }

            Log.d(TAG, "All views initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            e.printStackTrace();
            finish();
        }
    }

    private void setInitialSelection(RadioGroup radioTimeGroup) {
        int radioId;

        if (selectedTimeMinutes == 1) {
            radioId = R.id.radio1min;
        } else if (selectedTimeMinutes == 3) {
            radioId = R.id.radio3min;
        } else if (selectedTimeMinutes == 5) {
            radioId = R.id.radio5min;
        } else if (selectedTimeMinutes == 10) {
            radioId = R.id.radio10min;
        } else if (selectedTimeMinutes == 30) {
            radioId = R.id.radio30min;
        } else if (selectedTimeMinutes == 0) {
            radioId = R.id.radioUnlimited;
        } else {
            radioId = R.id.radio10min;
        }

        radioTimeGroup.check(radioId);
        Log.d(TAG, "Initial selection set to: " + selectedTimeMinutes + " minutes");
    }

    private void handleTimeSelection(int checkedId) {
        if (checkedId == R.id.radio1min) {
            selectedTimeMinutes = 1;
        } else if (checkedId == R.id.radio3min) {
            selectedTimeMinutes = 3;
        } else if (checkedId == R.id.radio5min) {
            selectedTimeMinutes = 5;
        } else if (checkedId == R.id.radio10min) {
            selectedTimeMinutes = 10;
        } else if (checkedId == R.id.radio30min) {
            selectedTimeMinutes = 30;
        } else if (checkedId == R.id.radioUnlimited) {
            selectedTimeMinutes = 0;
        }
        Log.d(TAG, "Time selected: " + selectedTimeMinutes + " minutes");
    }

    private void setupListeners() {
    }

    private void returnToColorSelection() {
        Log.d(TAG, "Returning to ColorSelection with time: " + selectedTimeMinutes);

        Intent resultIntent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putInt("selected_time_minutes", selectedTimeMinutes);
        bundle.putInt("selected_time_seconds", selectedTimeMinutes * 60);
        bundle.putBoolean("is_timed_game", selectedTimeMinutes > 0);
        resultIntent.putExtras(bundle);

        setResult(RESULT_OK, resultIntent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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