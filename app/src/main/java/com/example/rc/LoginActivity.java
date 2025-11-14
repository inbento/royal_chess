package com.example.rc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.rc.database.DatabaseHelper;
import com.example.rc.models.User;

import java.util.Locale;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin;

    private EditText etRegisterUsername, etRegisterEmail, etRegisterPassword, etRegisterConfirmPassword;
    private Button btnRegister;

    private Button btnLoginTab, btnRegisterTab;
    private LinearLayout loginForm, registerForm;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isUserLoggedIn()) {
            goToMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        initViews();
        setupButtonListeners();
        showLoginForm();
    }

    private void initViews() {
        loginForm = findViewById(R.id.loginForm);
        registerForm = findViewById(R.id.registerForm);

        btnLoginTab = findViewById(R.id.btnLoginTab);
        btnRegisterTab = findViewById(R.id.btnRegisterTab);

        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);

        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
    }

    private void setupButtonListeners() {
        btnLoginTab.setOnClickListener(v -> showLoginForm());
        btnRegisterTab.setOnClickListener(v -> showRegisterForm());

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void showLoginForm() {
        loginForm.setVisibility(View.VISIBLE);
        registerForm.setVisibility(View.GONE);

        btnLoginTab.setTextColor(getResources().getColor(android.R.color.white));
        btnLoginTab.setBackgroundResource(R.drawable.btn_tab_selected);

        btnRegisterTab.setTextColor(getResources().getColor(android.R.color.black));
        btnRegisterTab.setBackgroundResource(R.drawable.btn_tab_unselected);
    }

    private void showRegisterForm() {
        loginForm.setVisibility(View.GONE);
        registerForm.setVisibility(View.VISIBLE);

        btnLoginTab.setTextColor(getResources().getColor(android.R.color.black));
        btnLoginTab.setBackgroundResource(R.drawable.btn_tab_unselected);

        btnRegisterTab.setTextColor(getResources().getColor(android.R.color.white));
        btnRegisterTab.setBackgroundResource(R.drawable.btn_tab_selected);
    }

    private void loginUser() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (!validateLoginForm(email, password)) {
            return;
        }

        User user = dbHelper.authenticateUser(email, password);
        if (user != null) {
            setUserLoggedIn(true);
            saveCurrentUserId(user.getId());
            goToMainActivity();
            Toast.makeText(this, "Добро пожаловать, " + user.getUsername() + "!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Неверный email или пароль", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser() {
        String username = etRegisterUsername.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String confirmPassword = etRegisterConfirmPassword.getText().toString().trim();

        if (!validateRegisterForm(username, email, password, confirmPassword)) {
            return;
        }

        // Проверяем, не занят ли email
        if (dbHelper.isEmailExists(email)) {
            etRegisterEmail.setError("Этот email уже зарегистрирован");
            return;
        }

        // Проверяем, не занято ли имя пользователя
        if (dbHelper.isUsernameExists(username)) {
            etRegisterUsername.setError("Это имя пользователя уже занято");
            return;
        }

        // Получаем текущего пользователя (если есть)
        User existingUser = dbHelper.getCurrentUser();

        if (existingUser != null) {
            boolean success = dbHelper.updateUserProfile(existingUser.getId(), username, email, password);
            if (success) {
                User updatedUser = dbHelper.getUserByEmail(email);
                if (updatedUser != null) {
                    setUserLoggedIn(true);
                    saveCurrentUserId(updatedUser.getId());
                    goToMainActivity();
                    Toast.makeText(this, "Аккаунт успешно обновлен!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Ошибка при обновлении аккаунта", Toast.LENGTH_SHORT).show();
            }
        } else {
            User newUser = new User(username, email, password);
            long result = dbHelper.addUser(newUser);

            if (result != -1) {
                User createdUser = dbHelper.getUserByEmail(email);
                if (createdUser != null) {
                    setUserLoggedIn(true);
                    saveCurrentUserId(createdUser.getId());
                    goToMainActivity();
                    Toast.makeText(this, "Аккаунт успешно создан!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Ошибка при создании аккаунта", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean validateLoginForm(String email, String password) {
        if (email.isEmpty()) {
            etLoginEmail.setError("Введите email");
            return false;
        }

        if (!isValidEmail(email)) {
            etLoginEmail.setError("Введите корректный email");
            return false;
        }

        if (password.isEmpty()) {
            etLoginPassword.setError("Введите пароль");
            return false;
        }

        if (password.length() < 6) {
            etLoginPassword.setError("Пароль должен содержать минимум 6 символов");
            return false;
        }

        return true;
    }

    private boolean validateRegisterForm(String username, String email, String password, String confirmPassword) {
        if (username.isEmpty()) {
            etRegisterUsername.setError("Введите имя пользователя");
            return false;
        }

        if (username.length() < 3) {
            etRegisterUsername.setError("Имя должно содержать минимум 3 символа");
            return false;
        }

        if (username.length() > 10) {
            etRegisterUsername.setError("Имя не должно превышать 10 символов");
            return false;
        }

        if (email.isEmpty()) {
            etRegisterEmail.setError("Введите email");
            return false;
        }

        if (!isValidEmail(email)) {
            etRegisterEmail.setError("Введите корректный email");
            return false;
        }

        if (password.isEmpty()) {
            etRegisterPassword.setError("Введите пароль");
            return false;
        }

        if (password.length() < 6) {
            etRegisterPassword.setError("Пароль должен содержать минимум 6 символов");
            return false;
        }

        if (confirmPassword.isEmpty()) {
            etRegisterConfirmPassword.setError("Подтвердите пароль");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etRegisterConfirmPassword.setError("Пароли не совпадают");
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getBoolean("isLoggedIn", false);
    }

    private void setUserLoggedIn(boolean loggedIn) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isLoggedIn", loggedIn).apply();
    }

    private void saveCurrentUserId(int userId) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putInt("currentUserId", userId).apply();
    }

    private int getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("currentUserId", -1);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
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