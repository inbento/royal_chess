package com.example.rc.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.rc.LoginActivity;
import com.example.rc.R;
import com.example.rc.database.DatabaseHelper;
import com.example.rc.models.GameStat;
import com.example.rc.models.User;

import java.util.List;

public class StatisticFragment extends Fragment {

    private TextView statsText;
    private Button btnEditProfile, btnLogout;
    private DatabaseHelper dbHelper;
    private int userId;
    private String username;
    private User currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistic, container, false);

        dbHelper = new DatabaseHelper(getContext());

        Bundle args = getArguments();
        if (args != null) {
            userId = args.getInt("userId", -1);
            username = args.getString("username", "Гость");
            currentUser = dbHelper.getUser(userId);
        }

        initViews(view);
        setupButtonListeners();
        loadStatistics();

        return view;
    }

    private void initViews(View view) {
        statsText = view.findViewById(R.id.statsText);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void setupButtonListeners() {
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showEditProfileDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Редактировать профиль");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);

        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);

        if (currentUser != null) {
            etUsername.setText(currentUser.getUsername());
            etEmail.setText(currentUser.getEmail());
        } else {
            etUsername.setText(username);
        }

        builder.setView(dialogView);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newUsername = etUsername.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPassword = etPassword.getText().toString().trim();

            if (validateProfileData(newUsername, newEmail, newPassword)) {
                updateUserProfile(newUsername, newEmail, newPassword);
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private boolean validateProfileData(String username, String email, String password) {
        if (username.isEmpty()) {
            Toast.makeText(getContext(), "Введите имя пользователя", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (username.length() < 3) {
            Toast.makeText(getContext(), "Имя должно содержать минимум 3 символа", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Введите корректный email", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.isEmpty() && password.length() < 6) {
            Toast.makeText(getContext(), "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void updateUserProfile(String newUsername, String newEmail, String newPassword) {
        boolean success = dbHelper.updateUserProfile(userId, newUsername, newEmail,
                newPassword.isEmpty() ? null : newPassword);

        if (success) {
            username = newUsername;

            if (currentUser != null) {
                currentUser.setUsername(newUsername);
                currentUser.setEmail(newEmail);
                if (!newPassword.isEmpty()) {
                    currentUser.setPassword(newPassword);
                }
            }

            Toast.makeText(getContext(), "Профиль обновлен!", Toast.LENGTH_SHORT).show();

            if (getActivity() != null) {
                TextView title = getActivity().findViewById(R.id.titleStats);
                if (title != null) {
                    title.setText(newUsername);
                }
            }

            if (getContext() != null) {
                Intent intent = new Intent("USERNAME_UPDATED");
                getContext().sendBroadcast(intent);
            }
        } else {
            Toast.makeText(getContext(), "Ошибка при обновлении профиля", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Выход из профиля")
                .setMessage("Вы уверены, что хотите выйти? Все данные статистики сохранятся.")
                .setPositiveButton("Выйти", (dialog, which) -> logoutUser())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void logoutUser() {
        SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isLoggedIn", false).apply();
        prefs.edit().remove("currentUserId").apply();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void loadStatistics() {
        if (userId == -1) {
            statsText.setText("Пользователь не найден");
            return;
        }

        List<GameStat> userStats = dbHelper.getUserStats(userId);
        int totalGames = dbHelper.getTotalGames(userId);
        int wins = dbHelper.getWins(userId);
        int losses = totalGames - wins;
        int winPercentage = totalGames > 0 ? (wins * 100 / totalGames) : 0;

        StringBuilder statsBuilder = new StringBuilder();
        statsBuilder.append("Общая статистика:\n\n");
        statsBuilder.append("Всего игр: ").append(totalGames).append("\n");
        statsBuilder.append("Побед: ").append(wins).append("\n");
        statsBuilder.append("Поражений: ").append(losses).append("\n");
        statsBuilder.append("Процент побед: ").append(winPercentage).append("%\n\n");

        if (currentUser != null) {
            statsBuilder.append("Информация о профиле:\n");
            statsBuilder.append("Имя: ").append(currentUser.getUsername()).append("\n");
            statsBuilder.append("Email: ").append(currentUser.getEmail()).append("\n\n");
        }

        if (!userStats.isEmpty()) {
            statsBuilder.append("Последние игры:\n");
            int count = Math.min(userStats.size(), 5);
            for (int i = 0; i < count; i++) {
                GameStat stat = userStats.get(i);
                String resultEmoji = stat.getResult().equals("win") ? "✅" : "❌";
                String colorText = stat.getColor().equals("white") ? "Белые" : "Черные";
                String duration = formatDuration(stat.getDuration());
                statsBuilder.append(resultEmoji)
                        .append(" ").append(colorText)
                        .append(" (").append(stat.getMovesCount()).append(" ходов, ")
                        .append(duration).append(")\n");
            }
        } else {
            statsBuilder.append("🎮 Сыграйте первую игру, чтобы увидеть статистику!");
        }

        statsText.setText(statsBuilder.toString());
    }

    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }
}