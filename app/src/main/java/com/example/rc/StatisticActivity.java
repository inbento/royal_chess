package com.example.rc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.rc.database.DatabaseHelper;
import com.example.rc.fragments.StatisticFragment;
import com.example.rc.models.User;

import java.util.Locale;

public class StatisticActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private User currentUser;
    private TextView titleStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistic_container);

        dbHelper = new DatabaseHelper(this);
        currentUser = dbHelper.getCurrentUser();

        initViews();
        setupUserData();
        setupFragments();
    }

    private void initViews() {
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        titleStats = findViewById(R.id.titleStats);
    }

    private void setupUserData() {
        if (currentUser != null) {
            titleStats.setText(currentUser.getUsername());
        } else {
            titleStats.setText("Гость");
        }
    }

    private void setupFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        StatisticFragment statisticFragment = new StatisticFragment();

        Bundle args = new Bundle();
        if (currentUser != null) {
            args.putInt("userId", currentUser.getId());
            args.putString("username", currentUser.getUsername());
        }
        statisticFragment.setArguments(args);

        transaction.replace(R.id.fragmentContainer, statisticFragment);
        transaction.commit();
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