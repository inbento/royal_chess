package com.example.rc.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.rc.models.GameStat;
import com.example.rc.models.User;
import com.example.rc.utils.PasswordHasher;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ChessApp.db";
    private static final int DATABASE_VERSION = 6;

    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_ONLINE_ID = "online_id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_SALT = "salt";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_SELECTED_KING = "selected_king";

    private static final String TABLE_STATS = "game_stats";
    private static final String COLUMN_STAT_ID = "id";
    private static final String COLUMN_USER_ID_FK = "user_id";
    private static final String COLUMN_RESULT = "result";
    private static final String COLUMN_COLOR = "color";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_MOVES_COUNT = "moves_count";



    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_ONLINE_ID + " TEXT UNIQUE,"
                + COLUMN_USERNAME + " TEXT UNIQUE,"
                + COLUMN_EMAIL + " TEXT UNIQUE,"
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_SALT + " TEXT,"
                + COLUMN_SELECTED_KING + " TEXT DEFAULT 'human',"
                + COLUMN_CREATED_AT + " TEXT"
                + ")";
        db.execSQL(createUsersTable);

        String createStatsTable = "CREATE TABLE " + TABLE_STATS + "("
                + COLUMN_STAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID_FK + " INTEGER,"
                + COLUMN_RESULT + " TEXT,"
                + COLUMN_COLOR + " TEXT,"
                + COLUMN_DURATION + " INTEGER,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_MOVES_COUNT + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_USER_ID_FK + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
                + ")";
        db.execSQL(createStatsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public String generateUniqueOnlineId() {
        return "user_" + System.currentTimeMillis() + "_" + new Random().nextInt(10000);
    }

    public long addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        String salt = PasswordHasher.generateSalt();
        String hashedPassword = PasswordHasher.hashPassword(user.getPassword(), salt);
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, user.getUsername());
        values.put(COLUMN_ONLINE_ID, generateUniqueOnlineId());
        values.put(COLUMN_EMAIL, user.getEmail());
        values.put(COLUMN_PASSWORD, hashedPassword);
        values.put(COLUMN_SALT, salt);
        values.put(COLUMN_CREATED_AT, user.getCreatedAt());

        return db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getUserOnlineId(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_ONLINE_ID},
                COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String onlineId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ONLINE_ID));
            cursor.close();
            return onlineId;
        }
        return null;
    }

    public User getUser(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            User user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
            user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)));
            user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)));
            user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
            cursor.close();
            return user;
        }
        return null;
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_EMAIL + " = ?",
                new String[]{email}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            User user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
            user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)));
            user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD))); // это теперь хеш
            user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
            cursor.close();
            return user;
        }
        return null;
    }

    public User authenticateUser(String email, String password) {
        User user = getUserByEmail(email);
        if (user != null) {
            String salt = getUserSalt(user.getId());
            if (PasswordHasher.verifyPassword(password, salt, user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    private String getUserSalt(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_SALT},
                COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String salt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SALT));
            cursor.close();
            return salt;
        }
        return null;
    }

    public boolean isEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_EMAIL + " = ?",
                new String[]{email});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    public boolean updateUserProfile(int userId, String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_EMAIL, email);

        if (password != null && !password.isEmpty()) {
            String newSalt = PasswordHasher.generateSalt();
            String hashedPassword = PasswordHasher.hashPassword(password, newSalt);
            values.put(COLUMN_PASSWORD, hashedPassword);
            values.put(COLUMN_SALT, newSalt);
        }

        int rowsAffected = db.update(TABLE_USERS, values, COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        return rowsAffected > 0;
    }

    public User getCurrentUser(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            User user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
            user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)));
            user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)));
            user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
            cursor.close();
            return user;
        }
        return null;
    }

    public boolean updateUsername(int userId, String newUsername) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, newUsername);
        int rowsAffected = db.update(TABLE_USERS, values, COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        return rowsAffected > 0;
    }

    public long addGameStat(GameStat stat) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID_FK, stat.getUserId());
        values.put(COLUMN_RESULT, stat.getResult());
        values.put(COLUMN_COLOR, stat.getColor());
        values.put(COLUMN_DURATION, stat.getDuration());
        values.put(COLUMN_DATE, stat.getDate());
        values.put(COLUMN_MOVES_COUNT, stat.getMovesCount());
        return db.insert(TABLE_STATS, null, values);
    }

    public List<GameStat> getUserStats(int userId) {
        List<GameStat> stats = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STATS, null, COLUMN_USER_ID_FK + " = ?",
                new String[]{String.valueOf(userId)}, null, null, COLUMN_DATE + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                GameStat stat = new GameStat();
                stat.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STAT_ID)));
                stat.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID_FK)));
                stat.setResult(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RESULT)));
                stat.setColor(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLOR)));
                stat.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DURATION)));
                stat.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                stat.setMovesCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MOVES_COUNT)));
                stats.add(stat);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return stats;
    }

    public int getTotalGames(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_STATS + " WHERE " + COLUMN_USER_ID_FK + " = ?",
                new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int getWins(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_STATS + " WHERE " + COLUMN_USER_ID_FK + " = ? AND " + COLUMN_RESULT + " = 'win'",
                new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_USERNAME + " = ?",
                new String[]{username});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    public boolean updateSelectedKing(int userId, String kingType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SELECTED_KING, kingType);

        Log.d("DatabaseHelper", "Updating king for user " + userId + " to: " + kingType);

        int rowsAffected = db.update(TABLE_USERS, values, COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});

        Log.d("DatabaseHelper", "Rows affected: " + rowsAffected);

        return rowsAffected > 0;
    }

    public String getSelectedKing(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_SELECTED_KING},
                COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String king = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SELECTED_KING));
            cursor.close();
            return king;
        }
        return "human";
    }

}