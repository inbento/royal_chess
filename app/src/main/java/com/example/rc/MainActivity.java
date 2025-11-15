package com.example.rc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.rc.database.DatabaseHelper;
import com.example.rc.models.GameStat;
import com.example.rc.models.User;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button btnStart, btnKings, btnUser, btnExit;
    private ImageButton btnLanguage, btnRules;
    private String currentLanguage = "ru";
    private DatabaseHelper dbHelper;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isUserLoggedIn()) {
            goToLoginActivity();
            return;
        }

        loadLanguagePreference();
        setLocale(currentLanguage);

        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        loadCurrentUser();

        initViews();
        setupButtonListeners();
        setupBackPressedHandler();
        updateUserButton();
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getBoolean("isLoggedIn", false);
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentUser();
        updateUserButton();
    }

    private int getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("currentUserId", -1);
    }

    private void loadCurrentUser() {
        int userId = getCurrentUserId();
        if (userId != -1) {
            currentUser = dbHelper.getUser(userId);
        } else {
            currentUser = dbHelper.getCurrentUser();
        }
    }
    private void initViews() {
        btnLanguage = findViewById(R.id.btnLanguage);
        btnStart = findViewById(R.id.btnStart);
        btnKings = findViewById(R.id.btnKings);
        btnUser = findViewById(R.id.btnStats);
        btnExit = findViewById(R.id.btnExit);
        btnRules = findViewById(R.id.btnRules);
    }


    private void updateUserButton() {
        if (currentUser != null) {
            btnUser.setText(currentUser.getUsername());
        }
    }
    private void loadLanguagePreference() {
        SharedPreferences preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        currentLanguage = preferences.getString("language", "ru");
    }

    private void saveLanguagePreference(String languageCode) {
        getSharedPreferences("AppSettings", MODE_PRIVATE)
                .edit()
                .putString("language", languageCode)
                .apply();
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                exitAppWithConfirmation();
            }
        });
    }

    private void setupButtonListeners() {
        btnLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageDialog();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        btnKings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openKingsScreen();
            }
        });

        btnUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUserProfile();
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitAppWithConfirmation();
            }
        });

        btnRules.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChessRules();
            }
        });
    }

    private void showLanguageDialog() {
        String[] languages = {
                getString(R.string.russian) + " (Русский)",
                getString(R.string.english) + " (English)"
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.select_language)
                .setItems(languages, (dialog, which) -> {
                    switch (which) {
                        case 0: // Русский
                            changeLanguage("ru");
                            break;
                        case 1: // Английский
                            changeLanguage("en");
                            break;
                    }
                })
                .show();
    }

    private void changeLanguage(String languageCode) {
        if (currentLanguage.equals(languageCode)) {
            Toast.makeText(this,
                    languageCode.equals("ru") ? "Уже выбран Русский" : "Already selected English",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        currentLanguage = languageCode;
        saveLanguagePreference(languageCode);
        setLocale(languageCode);

        recreate();
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
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

    private void startGame() {
        Intent intent = new Intent(MainActivity.this, GameModeSelectionActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void openKingsScreen() {
        Intent intent = new Intent(MainActivity.this, KingsActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void openUserProfile() {
        Intent intent = new Intent(MainActivity.this, StatisticActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void openChessRules() {
        String rulesUrl = getString(R.string.chess_rules_url);

        Log.d("ChessRules", "Opening URL: " + rulesUrl);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(rulesUrl));
        Intent chooser = Intent.createChooser(intent, getString(R.string.open_with));

        if (chooser.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        } else {
            Toast.makeText(this, getString(R.string.no_browser), Toast.LENGTH_LONG).show();
        }
    }

    private void exitAppWithConfirmation() {
        finishAffinity();
        System.exit(0);
    }
}