package com.example.rc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rc.adapters.PromotionAdapter;
import com.example.rc.chess.ChessBoard;
import com.example.rc.chess.ChessPiece;
import com.example.rc.chess.ChessTimer;
import com.example.rc.chess.pieces.Knight;
import com.example.rc.chess.pieces.Pawn;
import com.example.rc.database.DatabaseHelper;
import com.example.rc.models.ChessMove;
import com.example.rc.models.GameStat;
import com.example.rc.models.User;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChessGameActivity extends AppCompatActivity
        implements PromotionAdapter.OnPromotionPieceSelected, ChessTimer.TimerListener {

    private ChessBoard chessBoard;
    private GridLayout chessGrid;
    private TextView tvCurrentPlayer;
    private Button btnBack, btnRestart;
    private boolean isPlayerWhite;
    private ChessSquare[][] squares = new ChessSquare[8][8];
    private RecyclerView rvPromotion;
    private PromotionAdapter promotionAdapter;
    private List<String> moves = new ArrayList<>();
    private int movesCount = 0;
    private LinearLayout promotionDialog;

    private int promotionRow = -1;
    private int promotionCol = -1;

    private ChessTimer chessTimer;
    private TextView tvWhiteTimer, tvBlackTimer;
    private View indicatorWhite, indicatorBlack;
    private boolean isTimedGame = true;
    private long gameTimeSeconds = 600;
    private long gameStartTime;

    private ImageView ivPlayerKing, ivOpponentKing;
    private String playerKingType, opponentKingType;

    private boolean isOnlineGame = false;
    private String opponentUsername = "Оппонент";
    private TextView tvPlayerName, tvOpponentName;
    private String sessionId;
    private String currentUserId;
    private ChildEventListener movesListener;
    private ValueEventListener boardStateListener;
    private ChildEventListener abilityListener;
    private ChildEventListener transformationListener;
    private ChildEventListener dragonFireListener;
    private ValueEventListener playerExitListener;
    private boolean isMyTurn = false;
    private int promotionFromRow = -1;
    private int promotionFromCol = -1;
    private boolean isGameFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chess_game);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isPlayerWhite = extras.getBoolean("player_color_white", true);
            gameTimeSeconds = extras.getInt("game_time_seconds", 600);
            isTimedGame = extras.getBoolean("is_timed_game", true);
            isOnlineGame = extras.getBoolean("is_online_game", false);

            if (extras.containsKey("session_id")) {
                isOnlineGame = true;
                sessionId = extras.getString("session_id");
                opponentUsername = extras.getString("opponent_username", "Соперник");
                opponentKingType = extras.getString("opponent_king_type", "human");

                Log.d("ChessGameActivity", "Online game detected - Session: " + sessionId);
            }

            Log.d("ChessGameActivity", "Timer settings - isTimedGame: " + isTimedGame +
                    ", gameTimeSeconds: " + gameTimeSeconds);
            Log.d("ChessGameActivity", "Intent extras received:");
            for (String key : extras.keySet()) {
                Log.d("ChessGameActivity", key + " = " + extras.get(key));
            }

            if (isOnlineGame) {
                sessionId = extras.getString("session_id");
                opponentUsername = extras.getString("opponent_username", "Соперник");
                opponentKingType = extras.getString("opponent_king_type", "human");

                DatabaseHelper dbHelper = new DatabaseHelper(this);
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                int userId = prefs.getInt("currentUserId", -1);
                User currentUser = dbHelper.getUser(userId);
                currentUserId = currentUser != null ? currentUser.getOnlineId() : "unknown";
                new android.os.Handler().postDelayed(() -> {
                    syncTurnWithServer();
                }, 1000);
            }
        }

        gameStartTime = System.currentTimeMillis();
        movesCount = 0;

        initViews();
        setupChessBoard();

        if (isOnlineGame) {
            setupOnlineUI();
        } else {
            setupOfflineUI();
        }

        if (isTimedGame && gameTimeSeconds > 0) {
            setupTimer();
            Log.d("ChessGameActivity", "Timer initialized successfully");
        } else {
            View timerWhiteLayout = findViewById(R.id.timerWhiteLayout);
            View timerBlackLayout = findViewById(R.id.timerBlackLayout);
            if (timerWhiteLayout != null) timerWhiteLayout.setVisibility(View.GONE);
            if (timerBlackLayout != null) timerBlackLayout.setVisibility(View.GONE);
            Log.d("ChessGameActivity", "Timer disabled - isTimedGame: " + isTimedGame +
                    ", gameTimeSeconds: " + gameTimeSeconds);
        }

        setupAdapters();
        updatePlayerTurn();
        loadSelectedKing();
        initKingView();
        if (isOnlineGame) {
            setupOnlineGame();
        }
    }

    private void initViews() {
        chessGrid = findViewById(R.id.chessGrid);
        tvCurrentPlayer = findViewById(R.id.tvCurrentPlayer);
        btnBack = findViewById(R.id.btnBack);
        btnRestart = findViewById(R.id.btnRestart);
        if (isOnlineGame) {
            btnRestart.setVisibility(View.GONE);
        }

        rvPromotion = findViewById(R.id.rvPromotion);
        promotionDialog = findViewById(R.id.promotionDialog);

        chessGrid.setColumnCount(8);
        chessGrid.setRowCount(8);

        btnBack.setOnClickListener(v -> {
            if (isOnlineGame) {
                showExitConfirmationDialog();
            } else {
                finish();
            }
        });
        btnRestart.setOnClickListener(v -> restartGame());

        if (promotionDialog != null) {
            promotionDialog.setVisibility(View.GONE);
        }

        tvWhiteTimer = findViewById(R.id.tvWhiteTimer);
        tvBlackTimer = findViewById(R.id.tvBlackTimer);
        indicatorWhite = findViewById(R.id.indicatorWhite);
        indicatorBlack = findViewById(R.id.indicatorBlack);
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvOpponentName = findViewById(R.id.tvOpponentName);
        ivPlayerKing = findViewById(R.id.ivPlayerKing);
        ivOpponentKing = findViewById(R.id.ivOpponentKing);

        ivPlayerKing.setOnClickListener(v -> {
            activateKingAbility();
        });

        ivOpponentKing.setOnClickListener(v -> {
            activateKingAbility();
        });
    }

    private void checkGameStatus() {
        if (!isOnlineGame) return;

        FirebaseManager.getInstance().getGameSession(sessionId, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String status = dataSnapshot.child("status").getValue(String.class);
                    if ("finished".equals(status) || "abandoned".equals(status)) {
                        runOnUiThread(() -> {
                            Toast.makeText(ChessGameActivity.this, "Эта игра уже завершена", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ChessGameActivity", "Error checking game status: " + databaseError.getMessage());
            }
        });
    }

    private void setupOnlineGame() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("currentUserId", -1);
        User currentUser = dbHelper.getUser(userId);

        if (currentUser != null) {
            currentUserId = dbHelper.getUserOnlineId(userId);
            Log.d("ChessGameActivity", "Current user online ID: " + currentUserId);
        }

        if (currentUserId == null) {
            Log.e("ChessGameActivity", "Failed to get current user online ID");
            currentUserId = "unknown_" + System.currentTimeMillis();
        }

        Log.d("ChessGameActivity", "Connecting to session: " + sessionId);

        checkGameStatus();

        setupBoardStateListener();
        setupMovesListener();
        setupAbilityListeners();
        setupTransformationListeners();
        setupDragonFireListeners();
        setupPlayerExitListener();
        determineTurn();
    }

    private void setupAbilityListeners() {
        abilityListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                try {
                    Map<String, Object> abilityData = (Map<String, Object>) dataSnapshot.getValue();
                    if (abilityData != null) {
                        String playerId = (String) abilityData.get("playerId");
                        String kingType = (String) abilityData.get("kingType");
                        Boolean isWhiteTurn = (Boolean) abilityData.get("isWhiteTurn");

                        if (playerId != null && !playerId.equals(currentUserId) && isWhiteTurn != null) {
                            applyOpponentAbilityActivation(kingType, isWhiteTurn);
                        }
                    }
                } catch (Exception e) {
                    Log.e("ChessGameActivity", "Error processing ability activation", e);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ChessGameActivity", "Ability listener cancelled: " + databaseError.getMessage());
            }
        };

        FirebaseManager.getInstance().listenForAbilityActivations(sessionId, abilityListener);
    }

    private void setupTransformationListeners() {
        transformationListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                try {
                    Map<String, Object> transformData = (Map<String, Object>) dataSnapshot.getValue();
                    if (transformData != null) {
                        String playerId = (String) transformData.get("playerId");
                        Long rowLong = (Long) transformData.get("row");
                        Long colLong = (Long) transformData.get("col");
                        String newPieceType = (String) transformData.get("newPieceType");
                        Boolean changeColor = (Boolean) transformData.get("changeColor");

                        if (playerId != null && !playerId.equals(currentUserId) &&
                                rowLong != null && colLong != null) {
                            applyOpponentPieceTransformation(
                                    rowLong.intValue(),
                                    colLong.intValue(),
                                    newPieceType,
                                    changeColor != null ? changeColor : false
                            );
                        }
                    }
                } catch (Exception e) {
                    Log.e("ChessGameActivity", "Error processing piece transformation", e);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ChessGameActivity", "Transformation listener cancelled: " + databaseError.getMessage());
            }
        };

        FirebaseManager.getInstance().listenForPieceTransformations(sessionId, transformationListener);
    }

    private void applyOpponentAbilityActivation(String kingType, boolean isWhiteTurn) {
        runOnUiThread(() -> {
            Log.d("ChessGameActivity", "Applying opponent ability: " + kingType + " for " + (isWhiteTurn ? "white" : "black"));

            if (isWhiteTurn) {
                chessBoard.activateKingAbilityForWhite(kingType);
            } else {
                chessBoard.activateKingAbilityForBlack(kingType);
            }
        });
    }

    private void applyOpponentPieceTransformation(int row, int col, String newPieceType, boolean changeColor) {
        runOnUiThread(() -> {
            Log.d("ChessGameActivity", "Applying opponent transformation at " + row + "," + col +
                    " to " + newPieceType + " changeColor: " + changeColor);

            ChessPiece originalPiece = chessBoard.getPiece(row, col);
            if (originalPiece != null) {
                boolean isWhite = originalPiece.isWhite();

                if (changeColor) {
                    isWhite = !isWhite;
                    Log.d("ChessGameActivity", "Color changed to: " + (isWhite ? "white" : "black"));
                }

                switch (newPieceType) {
                    case "knight":
                        chessBoard.getBoard()[row][col] = new Knight(isWhite, row, col);
                        break;
                    case "pawn":
                        chessBoard.getBoard()[row][col] = new Pawn(isWhite, row, col);
                        break;
                }

                updateBoard();

                if (chessBoard.getSelectedPiece() != null) {
                    chessBoard.selectPiece(chessBoard.getSelectedPiece().getRow(),
                            chessBoard.getSelectedPiece().getCol());
                    highlightPossibleMoves();
                }
            }
        });
    }

    private void setupBoardStateListener() {
        boardStateListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean isWhiteTurn = dataSnapshot.child("isWhiteTurn").getValue(Boolean.class);
                    if (isWhiteTurn != null) {
                        syncBoardState(isWhiteTurn);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ChessGameActivity", "Board state listener cancelled: " + databaseError.getMessage());
            }
        };

        FirebaseManager.getInstance().listenForBoardState(sessionId, boardStateListener);
    }

    private void setupMovesListener() {
        movesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                try {
                    Log.d("ChessGameActivity", "onChildAdded: " + dataSnapshot.getKey());
                    ChessMove move = dataSnapshot.getValue(ChessMove.class);
                    if (move != null) {
                        String movePlayerId = move.getPlayerId();
                        Log.d("ChessGameActivity", "Processing move from player: " + movePlayerId + ", my ID: " + currentUserId);

                        if (movePlayerId != null && !movePlayerId.equals(currentUserId)) {
                            applyOpponentMove(move);
                        } else if (movePlayerId == null) {
                            Log.w("ChessGameActivity", "Received move with null playerId");
                        }
                    } else {
                        Log.w("ChessGameActivity", "Received null move data");
                    }
                } catch (Exception e) {
                    Log.e("ChessGameActivity", "Error in onChildAdded", e);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ChessGameActivity", "Moves listener cancelled: " + databaseError.getMessage());
            }
        };

        FirebaseManager.getInstance().listenForMoves(sessionId, movesListener);
    }

    private void determineTurn() {
        if (isOnlineGame) {
            boolean isWhiteTurn = chessBoard.isWhiteTurn();
            isMyTurn = (isPlayerWhite && isWhiteTurn) || (!isPlayerWhite && !isWhiteTurn);

            Log.d("ChessGameActivity", "Online turn - " +
                    "isPlayerWhite: " + isPlayerWhite +
                    ", boardWhiteTurn: " + isWhiteTurn +
                    ", isMyTurn: " + isMyTurn);
        } else {
            isMyTurn = (isPlayerWhite && chessBoard.isWhiteTurn()) ||
                    (!isPlayerWhite && !chessBoard.isWhiteTurn());
        }
    }

    private void updateTurnIndicator() {
        runOnUiThread(() -> {
            if (isMyTurn) {
                tvCurrentPlayer.setText("Ваш ход");
                tvCurrentPlayer.setTextColor(Color.GREEN);
            } else {
                tvCurrentPlayer.setText("Ход противника");
                tvCurrentPlayer.setTextColor(Color.RED);
            }
        });
    }

    private void setupOnlineUI() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("currentUserId", -1);
        User currentUser = dbHelper.getUser(userId);

        String playerName = currentUser != null ? currentUser.getUsername() : "Игрок";

        if (isPlayerWhite) {
            tvPlayerName.setText(playerName);
            tvOpponentName.setText(opponentUsername);
        } else {
            tvPlayerName.setText(opponentUsername);
            tvOpponentName.setText(playerName);
        }

        tvPlayerName.setVisibility(View.VISIBLE);
        tvOpponentName.setVisibility(View.VISIBLE);

        setupKingsForOnline();
    }

    private void setupKingsForOnline() {
        loadSelectedKing();

        Log.d("ChessGameActivity", "setupKingsForOnline - Player King: " + playerKingType +
                ", Opponent King: " + opponentKingType +
                ", Is Player White: " + isPlayerWhite);

        new Handler().postDelayed(() -> {
            String whiteKingType, blackKingType;

            if (isPlayerWhite) {
                whiteKingType = playerKingType;
                blackKingType = opponentKingType;
            } else {
                whiteKingType = opponentKingType;
                blackKingType = playerKingType;
            }

            int whiteKingDrawable = getKingDrawableId(whiteKingType);
            int blackKingDrawable = getKingDrawableId(blackKingType);

            runOnUiThread(() -> {
                ivPlayerKing.setImageResource(whiteKingDrawable);
                ivOpponentKing.setImageResource(blackKingDrawable);

                Log.d("ChessGameActivity", "Kings UI updated - White: " + whiteKingType +
                        " (" + whiteKingDrawable + "), Black: " + blackKingType +
                        " (" + blackKingDrawable + ")");
            });

            if (chessBoard != null) {
                chessBoard.activateKingAbilityForWhite(whiteKingType);
                chessBoard.activateKingAbilityForBlack(blackKingType);

                Log.d("ChessGameActivity", "Abilities activated - White: " + whiteKingType +
                        ", Black: " + blackKingType);
            }
        }, 500);
    }

    private void loadSelectedKing() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        playerKingType = prefs.getString("selected_king_type", "human");
        Log.d("ChessGameActivity", "Loaded selected king: " + playerKingType);
    }

    private void syncKingDisplay() {
        if (!isOnlineGame) return;

        new Handler().postDelayed(() -> {
            String whiteKingType = isPlayerWhite ? playerKingType : opponentKingType;
            String blackKingType = isPlayerWhite ? opponentKingType : playerKingType;

            runOnUiThread(() -> {
                ivPlayerKing.setImageResource(getKingDrawableId(whiteKingType));
                ivOpponentKing.setImageResource(getKingDrawableId(blackKingType));

                Log.d("ChessGameActivity", "King display synced - White: " + whiteKingType +
                        ", Black: " + blackKingType);
            });
        }, 1000);
    }

    private void setupOfflineUI() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String whitePlayerName = extras.getString("white_player_name", "Игрок 1");
            String blackPlayerName = extras.getString("black_player_name", "Игрок 2");
            String whiteKingType = extras.getString("white_king_type", "human");
            String blackKingType = extras.getString("black_king_type", "human");

            tvPlayerName.setText(whitePlayerName);
            tvOpponentName.setText(blackPlayerName);

            int whiteKingDrawable = getKingDrawableId(whiteKingType);
            int blackKingDrawable = getKingDrawableId(blackKingType);

            ivPlayerKing.setImageResource(whiteKingDrawable);
            ivOpponentKing.setImageResource(blackKingDrawable);

            this.playerKingType = whiteKingType;
            this.opponentKingType = blackKingType;

            if (chessBoard != null) {
                chessBoard.activateKingAbilityForWhite(whiteKingType);
                chessBoard.activateKingAbilityForBlack(blackKingType);
            }
        }

        tvPlayerName.setVisibility(View.VISIBLE);
        tvOpponentName.setVisibility(View.VISIBLE);
    }

    private void setupKingsForOnline(String playerKingType, String opponentKingType) {
        int playerKingDrawable = getKingDrawableId(playerKingType);
        int opponentKingDrawable = getKingDrawableId(opponentKingType);

        ivPlayerKing.setImageResource(playerKingDrawable);
        ivOpponentKing.setImageResource(opponentKingDrawable);

        if (chessBoard != null) {
            if (isPlayerWhite) {
                chessBoard.activateKingAbilityForWhite(playerKingType);
                chessBoard.activateKingAbilityForBlack(opponentKingType);
            } else {
                chessBoard.activateKingAbilityForWhite(opponentKingType);
                chessBoard.activateKingAbilityForBlack(playerKingType);
            }
        }
    }

    private void setupKingsForOffline(String whiteKingType, String blackKingType) {
        int whiteKingDrawable = getKingDrawableId(whiteKingType);
        int blackKingDrawable = getKingDrawableId(blackKingType);

        ivPlayerKing.setImageResource(whiteKingDrawable);
        ivOpponentKing.setImageResource(blackKingDrawable);

        this.playerKingType = whiteKingType;
        this.opponentKingType = blackKingType;
    }

    private void initKingView() {
        ivPlayerKing = findViewById(R.id.ivPlayerKing);
        ivOpponentKing = findViewById(R.id.ivOpponentKing);

        Log.d("ChessGameActivity", "initKingView - playerKingType: " + playerKingType);

        int kingDrawableId = getKingDrawableId(playerKingType);
        ivPlayerKing.setImageResource(kingDrawableId);

        ivPlayerKing.setOnClickListener(v -> {
            Log.d("ChessGameActivity", "White king clicked, type: " + playerKingType);
            activateKingAbility();
        });

        ivOpponentKing.setOnClickListener(v -> {
            Log.d("ChessGameActivity", "Black king clicked, type: " + opponentKingType);
            activateKingAbility();
        });
    }

    private int getKingDrawableId(String kingType) {
        if (kingType == null) {
            Log.e("ChessGameActivity", "KingType is null, using default human king");
            return R.drawable.king_of_man_bg;
        }

        Log.d("ChessGameActivity", "Getting drawable for king: " + kingType);
        int drawableId;
        switch (kingType) {
            case "human":
                drawableId = R.drawable.king_of_man_bg;
                break;
            case "dragon":
                drawableId = R.drawable.king_of_dragon_bg;
                break;
            case "elf":
                drawableId = R.drawable.king_of_elf_bg;
                break;
            case "gnome":
                drawableId = R.drawable.king_of_gnom_bg;
                break;
            default:
                Log.w("ChessGameActivity", "Unknown king type: " + kingType + ", using default");
                drawableId = R.drawable.king_of_man_bg;
        }

        Log.d("ChessGameActivity", "Drawable ID for " + kingType + " = " + drawableId);
        return drawableId;
    }

    private void activateKingAbility() {
        if (chessBoard != null) {
            String currentKingType;

            if (chessBoard.isWhiteTurn()) {
                currentKingType = playerKingType;
            } else {
                currentKingType = opponentKingType;
            }


            boolean isCurrentPlayerTurn = (chessBoard.isWhiteTurn() && isPlayerWhite) ||
                    (!chessBoard.isWhiteTurn() && !isPlayerWhite);

            if (isOnlineGame && !isCurrentPlayerTurn) {
                Toast.makeText(this, "Сейчас не ваш ход для активации способности", Toast.LENGTH_SHORT).show();
                return;
            }

            if (chessBoard.isWhiteTurn()) {
                currentKingType = isPlayerWhite ? playerKingType : opponentKingType;
            } else {
                currentKingType = !isPlayerWhite ? playerKingType : opponentKingType;
            }

            chessBoard.activateKingAbility(currentKingType);
            if (isOnlineGame) {
                sendAbilityActivation(currentKingType);
            }
            String abilityName = getKingAbilityName(currentKingType);
            String message = "Активирована способность: " + abilityName;

            if ("elf".equals(currentKingType)) {
                message += "\nНажмите на свою пешку чтобы превратить её в коня";
            }
            else if ("human".equals(currentKingType)) {
                message += "\nНажмите на вражескую пешку на вашей половине доски чтобы превратить её в свою";
            }
            else if ("dragon".equals(currentKingType)) {
                message += "\nВыберите свою пешку и нажмите на вражескую фигуру по горизонтали/вертикали для выстрела";
            }
            else if ("gnome".equals(currentKingType)) {
                message += "\nПешки могут двигаться и бить назад (пассивная способность)";
            }

            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            if (hasSelectedPiece()) {
                chessBoard.selectPiece(getSelectedPieceRow(), getSelectedPieceCol());
                updateBoard();
                highlightPossibleMoves();
            }
        }
    }

    private void sendAbilityActivation(String kingType) {
        try {
            Map<String, Object> abilityData = new HashMap<>();
            abilityData.put("playerId", currentUserId);
            abilityData.put("kingType", kingType);
            abilityData.put("timestamp", System.currentTimeMillis());
            abilityData.put("isWhiteTurn", chessBoard.isWhiteTurn());

            FirebaseManager.getInstance().sendAbilityActivation(sessionId, abilityData);
        } catch (Exception e) {
            Log.e("ChessGameActivity", "Error sending ability activation: " + e.getMessage());
        }
    }

    private String getKingAbilityName(String kingType) {
        switch (kingType) {
            case "human": return "Боевое вдохновение";
            case "dragon": return "Дыхание дракона";
            case "elf": return "Лесная магия";
            case "gnome": return "Подземные ходы";
            default: return "Неизвестная способность";
        }
    }

    private void setupTimer() {
        long initialTimeMillis = gameTimeSeconds * 1000;
        chessTimer = new ChessTimer(initialTimeMillis, tvWhiteTimer, tvBlackTimer,
                indicatorWhite, indicatorBlack, this);
        chessTimer.start();
    }

    @Override
    public void onTimeOut(boolean isWhite) {
        String loser = isWhite ? "Белые" : "Черные";
        String winner = isWhite ? "Черные" : "Белые";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Время вышло!")
                .setMessage(loser + " проиграли по времени!\nПобедили: " + winner)
                .setPositiveButton("Новая игра", (dialog, which) -> restartGame())
                .setNegativeButton("Выход", (dialog, which) -> finish())
                .setCancelable(false)
                .show();

        chessTimer.stop();
    }

    @Override
    public void onTimeUpdate(long whiteTime, long blackTime) {}

    private void setupChessBoard() {

        chessBoard = new ChessBoard();

        if (chessGrid != null) {
            chessGrid.removeAllViews();
        }

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int availableHeight = screenHeight - 200;
        int cellSize = Math.min((screenWidth - 32) / 8, availableHeight / 8);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessSquare square = new ChessSquare(this, row, col);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                square.setLayoutParams(params);

                ChessPiece piece = chessBoard.getPiece(row, col);
                square.setPiece(piece);

                final int finalRow = row;
                final int finalCol = col;
                square.setOnClickListener(v -> handleSquareClick(finalRow, finalCol));

                squares[row][col] = square;
                chessGrid.addView(square);
            }
        }
    }

    private void setupAdapters() {

        if (rvPromotion != null) {
            boolean isCurrentPlayerWhite = chessBoard.isWhiteTurn();
            promotionAdapter = new PromotionAdapter(this, isCurrentPlayerWhite);
            rvPromotion.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvPromotion.setAdapter(promotionAdapter);
        }
    }

    private void handleSquareClick(int row, int col) {
        if (promotionDialog != null && promotionDialog.getVisibility() == View.VISIBLE) {
            return;
        }

        if (isOnlineGame && !isMyTurn) {
            Toast.makeText(this, "Сейчас не ваш ход", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chessBoard.isElfAbilityAvailable() && chessBoard.isElfAbilityActive()) {
            boolean success = chessBoard.activateElfAbility(row, col);
            if (success) {
                Toast.makeText(this, "Пешка превращена в коня!", Toast.LENGTH_SHORT).show();

                if (isOnlineGame) {
                    sendPieceTransformation(row, col, "knight", false);
                }

                updateBoard();
                return;
            }
        }

        if (chessBoard.isHumanAbilityAvailable() && chessBoard.isHumanAbilityActive()) {
            boolean success = chessBoard.activateHumanAbility(row, col);
            if (success) {
                int remaining = chessBoard.getHumanAbilityRemainingUses();
                String message = "Пешка перешла на вашу сторону!" +
                        (remaining > 0 ? "\nОсталось превращений: " + remaining : "");
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                if (isOnlineGame) {
                    sendPieceTransformation(row, col, "pawn", true);
                }

                updateBoard();
                return;
            }
        }

        if (chessBoard.isDragonFireShotAvailable() && chessBoard.isDragonAbilityActive() &&
                chessBoard.getSelectedPiece() != null) {
            ChessPiece selected = chessBoard.getSelectedPiece();
            if (selected != null && selected.getType() == ChessPiece.PieceType.PAWN) {
                boolean success = chessBoard.activateDragonFireShot(
                        selected.getRow(), selected.getCol(), row, col);
                if (success) {
                    Toast.makeText(this, "Огненный выстрел! Фигура сожжена!", Toast.LENGTH_SHORT).show();

                    if (isOnlineGame) {
                        sendDragonFireShot(selected.getRow(), selected.getCol(), row, col);
                    }

                    updateBoard();
                    clearAllSelection();
                    chessBoard.selectPiece(-1, -1);
                    return;
                }
            }
        }

        ChessPiece piece = chessBoard.getPiece(row, col);

        ChessPiece selectedPiece = chessBoard.getSelectedPiece();
        if (selectedPiece != null) {
            int fromRow = selectedPiece.getRow();
            int fromCol = selectedPiece.getCol();
            String moveType = "normal";

            if (chessBoard.movePiece(row, col)) {
                movesCount++;

                ChessPiece currentSelectedPiece = chessBoard.getSelectedPiece();
                if (currentSelectedPiece != null && currentSelectedPiece.getType() == ChessPiece.PieceType.KING &&
                        Math.abs(col - fromCol) == 2) {
                    moveType = "castling";
                }

                if (isTimedGame && chessTimer != null) {
                    chessTimer.switchTurn();
                }

                if (isPawnPromotion(row, col)) {
                    promotionFromRow = fromRow;
                    promotionFromCol = fromCol;

                    if (isTimedGame && chessTimer != null) {
                        chessTimer.pause();
                    }
                    showPromotionDialog(row, col);
                } else {
                    if (isOnlineGame) {
                        sendMoveToOpponent(fromRow, fromCol, row, col, -1, moveType);
                    }

                    updateBoard();
                    updatePlayerTurn();
                    updateOnlineTurn();
                    checkForCheckmate();

                    if (chessBoard.isCheckmate(!chessBoard.isWhiteTurn())) {
                        showGameOverDialog(chessBoard.isWhiteTurn());
                    }
                }
            } else {
                if (piece != null && piece.isWhite() == chessBoard.isWhiteTurn()) {
                    clearAllSelection();
                    chessBoard.selectPiece(row, col);
                    squares[row][col].setSelected(true);
                    highlightPossibleMoves();
                } else {
                    clearAllSelection();
                    chessBoard.selectPiece(-1, -1);
                }
            }
        } else {
            if (piece != null && piece.isWhite() == chessBoard.isWhiteTurn()) {
                if (chessBoard.selectPiece(row, col)) {
                    clearAllSelection();
                    squares[row][col].setSelected(true);
                    highlightPossibleMoves();
                }
            }
        }
    }
    private void sendDragonFireShot(int fromRow, int fromCol, int toRow, int toCol) {
        try {
            Map<String, Object> fireData = new HashMap<>();
            fireData.put("playerId", currentUserId);
            fireData.put("fromRow", fromRow);
            fireData.put("fromCol", fromCol);
            fireData.put("toRow", toRow);
            fireData.put("toCol", toCol);
            fireData.put("timestamp", System.currentTimeMillis());

            FirebaseManager.getInstance().sendDragonFireShot(sessionId, fireData);

            Log.d("ChessGameActivity", "Sent dragon fire shot from " + fromRow + "," + fromCol +
                    " to " + toRow + "," + toCol);
        } catch (Exception e) {
            Log.e("ChessGameActivity", "Error sending dragon fire shot: " + e.getMessage());
        }
    }

    private void setupDragonFireListeners() {
        dragonFireListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                try {
                    Map<String, Object> fireData = (Map<String, Object>) dataSnapshot.getValue();
                    if (fireData != null) {
                        String playerId = (String) fireData.get("playerId");
                        Long fromRow = (Long) fireData.get("fromRow");
                        Long fromCol = (Long) fireData.get("fromCol");
                        Long toRow = (Long) fireData.get("toRow");
                        Long toCol = (Long) fireData.get("toCol");

                        if (playerId != null && !playerId.equals(currentUserId) &&
                                fromRow != null && fromCol != null && toRow != null && toCol != null) {
                            applyOpponentDragonFireShot(
                                    fromRow.intValue(), fromCol.intValue(),
                                    toRow.intValue(), toCol.intValue()
                            );
                        }
                    }
                } catch (Exception e) {
                    Log.e("ChessGameActivity", "Error processing dragon fire shot", e);
                }
            }

            @Override public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {}
            @Override public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}
            @Override public void onCancelled(DatabaseError databaseError) {
                Log.e("ChessGameActivity", "Dragon fire listener cancelled: " + databaseError.getMessage());
            }
        };

        FirebaseManager.getInstance().listenForDragonFireShots(sessionId, dragonFireListener);
    }

    private void applyOpponentDragonFireShot(int fromRow, int fromCol, int toRow, int toCol) {
        runOnUiThread(() -> {
            Log.d("ChessGameActivity", "Applying opponent dragon fire from " + fromRow + "," + fromCol +
                    " to " + toRow + "," + toCol);

            chessBoard.getBoard()[toRow][toCol] = null;

            updateBoard();

            if (chessBoard.getSelectedPiece() != null) {
                chessBoard.selectPiece(chessBoard.getSelectedPiece().getRow(),
                        chessBoard.getSelectedPiece().getCol());
                highlightPossibleMoves();
            }

            Toast.makeText(ChessGameActivity.this, "Противник сжег вашу фигуру!", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendPieceTransformation(int row, int col, String newPieceType, boolean changeColor) {
        try {
            Map<String, Object> transformData = new HashMap<>();
            transformData.put("playerId", currentUserId);
            transformData.put("row", row);
            transformData.put("col", col);
            transformData.put("newPieceType", newPieceType);
            transformData.put("changeColor", changeColor);
            transformData.put("timestamp", System.currentTimeMillis());

            FirebaseManager.getInstance().sendPieceTransformation(sessionId, transformData);

            Log.d("ChessGameActivity", "Sent piece transformation: " + newPieceType +
                    " at " + row + "," + col + " changeColor: " + changeColor);
        } catch (Exception e) {
            Log.e("ChessGameActivity", "Error sending piece transformation: " + e.getMessage());
        }
    }

    private boolean isPawnPromotion(int row, int col) {
        ChessPiece piece = chessBoard.getPiece(row, col);
        if (piece != null && piece.getType() == ChessPiece.PieceType.PAWN) {
            return (piece.isWhite() && row == 0) || (!piece.isWhite() && row == 7);
        }
        return false;
    }

    private void showPromotionDialog(int row, int col) {
        promotionRow = row;
        promotionCol = col;
        if (promotionDialog != null) {

            ChessPiece piece = chessBoard.getPiece(row, col);
            boolean isPromotingPieceWhite = piece != null && piece.isWhite();
            promotionAdapter = new PromotionAdapter(this, isPromotingPieceWhite);
            if (rvPromotion != null) {
                rvPromotion.setAdapter(promotionAdapter);
            }
            promotionDialog.setVisibility(View.VISIBLE);
        }
    }

    private void hidePromotionDialog() {
        if (promotionDialog != null) {
            promotionDialog.setVisibility(View.GONE);
        }
        promotionRow = -1;
        promotionCol = -1;
    }

    @Override
    public void onPieceSelected(int pieceType) {
        if (promotionRow != -1 && promotionCol != -1) {
            chessBoard.promotePawn(promotionRow, promotionCol, pieceType);
            updateBoard();

            if (isTimedGame && chessTimer != null) {
                chessTimer.resume();
                chessTimer.switchTurn();
            }

            updatePlayerTurn();

            if (isOnlineGame) {
                sendMoveToOpponent(promotionFromRow, promotionFromCol, promotionRow, promotionCol, pieceType, "promotion");
                updateOnlineTurn();
            }

            hidePromotionDialog();

            if (chessBoard.isCheckmate(!chessBoard.isWhiteTurn())) {
                showGameOverDialog(chessBoard.isWhiteTurn());
            }

            promotionFromRow = -1;
            promotionFromCol = -1;
        }
    }

    private void clearAllSelection() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col].setSelected(false);
                squares[row][col].updateBaseColor();
            }
        }
    }

    private void highlightPossibleMoves() {
        for (int[] move : chessBoard.getPossibleMoves()) {
            int row = move[0];
            int col = move[1];
            ChessPiece target = chessBoard.getPiece(row, col);

            if (target == null) {
                squares[row][col].setBackgroundColor(Color.parseColor("#ADD8E6"));
            } else {
                squares[row][col].setBackgroundColor(Color.parseColor("#FFB6C1"));
            }
        }
    }

    private void updateBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = chessBoard.getPiece(row, col);
                squares[row][col].setPiece(piece);
                squares[row][col].setSelected(false);
            }
        }
    }

    private void updatePlayerTurn() {
        boolean isWhiteTurn = chessBoard.isWhiteTurn();
        String playerText;

        if ((isWhiteTurn && isPlayerWhite) || (!isWhiteTurn && !isPlayerWhite)) {
            playerText = getString(R.string.your_turn);
        } else {
            playerText = getString(R.string.opponent_turn);
        }

        String colorText = isWhiteTurn ? getString(R.string.white) : getString(R.string.black);

        if (chessBoard.isKingInCheck(isWhiteTurn)) {
            if (chessBoard.isCheckmate(isWhiteTurn)) {
                tvCurrentPlayer.setText("ШАХ И МАТ! Победили " + (!isWhiteTurn ? "Белые" : "Черные"));
                showGameOverDialog(!isWhiteTurn);
            } else {
                tvCurrentPlayer.setText(playerText + " (" + colorText + ") - ШАХ!");
            }
        } else {
            tvCurrentPlayer.setText(playerText + " (" + colorText + ")");
        }
    }

    private void showGameOverDialog(boolean isWhiteWinner) {
        if (isGameFinished) {
            return;
        }

        isGameFinished = true;

        if (isTimedGame && chessTimer != null) {
            chessTimer.stop();
        }

        int gameDuration = (int) ((System.currentTimeMillis() - gameStartTime) / 1000);

        saveGameResult(isWhiteWinner, gameDuration);

        if (isOnlineGame) {
            updateGameSessionStatus("finished", isWhiteWinner);

            new Handler().postDelayed(() -> {
                FirebaseManager.getInstance().deleteGameSession(sessionId);
            }, 30000);
        }

        runOnUiThread(() -> {
            try {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Игра окончена!")
                        .setMessage("ШАХ И МАТ!\nПобедили: " + (isWhiteWinner ? "Белые" : "Черные"))
                        .setPositiveButton("OK", (dialog, which) -> {
                            if (isOnlineGame) {
                                finish();
                            } else {
                                restartGame();
                            }
                        })
                        .setCancelable(false)
                        .setOnDismissListener(dialog -> {
                            isGameFinished = true;
                        })
                        .show();
            } catch (Exception e) {
                Log.e("ChessGameActivity", "Error showing game over dialog: " + e.getMessage());
            }
        });
    }

    private void updateGameSessionStatus(String status, boolean isWhiteWinner) {
        try {
            String winnerId = null;
            if (isWhiteWinner) {
                winnerId = chessBoard.isWhiteTurn() ? currentUserId : getOpponentId();
            } else {
                winnerId = !chessBoard.isWhiteTurn() ? currentUserId : getOpponentId();
            }

            FirebaseManager.getInstance().updateGameSessionStatus(
                    sessionId,
                    status,
                    winnerId,
                    movesCount
            );
        } catch (Exception e) {
            Log.e("ChessGameActivity", "Error updating game session status: " + e.getMessage());
        }
    }

    private String getOpponentId() {
        return "opponent_" + sessionId;
    }

    private void saveGameResult(boolean isWin, int durationSeconds) {
        if (isGameFinished) {
            Log.d("ChessGameActivity", "Game result already saved, ignoring duplicate");
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("currentUserId", -1);
        User currentUser = dbHelper.getUser(userId);

        if (currentUser != null) {
            String result = isWin ? "win" : "loss";
            String color = isPlayerWhite ? "white" : "black";

            GameStat stat = new GameStat(
                    currentUser.getId(),
                    result,
                    color,
                    durationSeconds,
                    this.movesCount
            );

            long statId = dbHelper.addGameStat(stat);
            Log.d("ChessGameActivity", "Game result saved with ID: " + statId + " - " + result + " as " + color);
        }
    }

    private void sendMoveToOpponent(int fromRow, int fromCol, int toRow, int toCol, int promotionType, String moveType) {
        try {
            if (currentUserId == null) {
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                int userId = prefs.getInt("currentUserId", -1);
                User currentUser = dbHelper.getUser(userId);
                currentUserId = currentUser != null ? dbHelper.getUserOnlineId(userId) : "unknown";
            }

            String safeMoveType = moveType != null ? moveType : "normal";
            ChessMove move = new ChessMove(currentUserId, fromRow, fromCol, toRow, toCol, promotionType, safeMoveType);

            Log.d("ChessGameActivity", "Sending move to opponent: " +
                    fromRow + "," + fromCol + " -> " + toRow + "," + toCol +
                    " Promotion: " + promotionType + " Type: " + safeMoveType +
                    " PlayerID: " + currentUserId);

            FirebaseManager.getInstance().sendMove(sessionId, move);

            FirebaseManager.getInstance().updateBoardState(sessionId,
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    chessBoard.isWhiteTurn());

        } catch (Exception e) {
            Log.e("ChessGameActivity", "Error sending move to opponent: " + e.getMessage());
            Toast.makeText(this, "Ошибка отправки хода", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyOpponentMove(ChessMove move) {
        runOnUiThread(() -> {
            try {
                Log.d("ChessGameActivity", "Applying opponent move: " +
                        move.getFromRow() + "," + move.getFromCol() + " -> " +
                        move.getToRow() + "," + move.getToCol() + " Type: " + move.getMoveType());

                clearAllSelection();
                chessBoard.selectPiece(-1, -1);

                ChessPiece sourcePiece = chessBoard.getPiece(move.getFromRow(), move.getFromCol());
                if (sourcePiece == null) {
                    Log.e("ChessGameActivity", "No piece found at source, syncing board state");
                    syncBoardWithServer();
                    return;
                }

                if (sourcePiece.isWhite() == isPlayerWhite) {
                    Log.w("ChessGameActivity", "Trying to move own piece, ignoring");
                    return;
                }

                if ("castling".equals(move.getMoveType())) {
                    applyCastlingMove(move);
                } else {
                    applyStandardMove(move);
                }
            } catch (Exception e) {
                Log.e("ChessGameActivity", "Error applying opponent move", e);
            }
        });
    }

    private void applyStandardMove(ChessMove move) {
        boolean selectionSuccess = chessBoard.selectPiece(move.getFromRow(), move.getFromCol());

        if (selectionSuccess && hasSelectedPiece()) {
            boolean moveSuccess = chessBoard.movePiece(move.getToRow(), move.getToCol());

            if (moveSuccess) {
                if (move.getPromotionType() != -1) {
                    chessBoard.promotePawn(move.getToRow(), move.getToCol(), move.getPromotionType());
                    Log.d("ChessGameActivity", "Applied pawn promotion to type: " + move.getPromotionType());
                }

                completeMoveApplication();
            } else {
                Log.e("ChessGameActivity", "Failed to apply opponent move - invalid move");
                applyMoveDirectly(move);
            }
        } else {
            Log.e("ChessGameActivity", "Failed to select opponent piece");
            applyMoveDirectly(move);
        }
    }

    private void applyMoveDirectly(ChessMove move) {
        try {
            ChessPiece piece = chessBoard.getPiece(move.getFromRow(), move.getFromCol());
            if (piece != null) {
                ChessPiece targetPiece = chessBoard.getPiece(move.getToRow(), move.getToCol());

                piece.setPosition(move.getToRow(), move.getToCol());
                chessBoard.getBoard()[move.getFromRow()][move.getFromCol()] = null;
                chessBoard.getBoard()[move.getToRow()][move.getToCol()] = piece;

                if (move.getPromotionType() != -1) {
                    chessBoard.promotePawn(move.getToRow(), move.getToCol(), move.getPromotionType());
                }

                chessBoard.setWhiteTurn(!chessBoard.isWhiteTurn());

                updateBoard();
                updatePlayerTurn();
                updateOnlineTurn();

                Log.d("ChessGameActivity", "Move applied directly");
            } else {
                Log.e("ChessGameActivity", "No piece to move in applyMoveDirectly");
            }
        } catch (Exception e) {
            Log.e("ChessGameActivity", "Error applying move directly: " + e.getMessage());
        }
    }

    private void applyCastlingMove(ChessMove move) {
        try {
            ChessPiece king = chessBoard.getPiece(move.getFromRow(), move.getFromCol());
            if (king != null && king.getType() == ChessPiece.PieceType.KING) {
                int direction = move.getToCol() > move.getFromCol() ? 1 : -1;
                int rookCol = direction == 1 ? 7 : 0;
                int newRookCol = direction == 1 ? move.getToCol() - 1 : move.getToCol() + 1;

                chessBoard.getBoard()[move.getFromRow()][move.getFromCol()] = null;
                chessBoard.getBoard()[move.getToRow()][move.getToCol()] = king;
                king.setPosition(move.getToRow(), move.getToCol());

                ChessPiece rook = chessBoard.getPiece(move.getToRow(), rookCol);
                if (rook != null && rook.getType() == ChessPiece.PieceType.ROOK) {
                    chessBoard.getBoard()[move.getToRow()][rookCol] = null;
                    chessBoard.getBoard()[move.getToRow()][newRookCol] = rook;
                    rook.setPosition(move.getToRow(), newRookCol);
                }

                chessBoard.setWhiteTurn(!chessBoard.isWhiteTurn());
                completeMoveApplication();

                Log.d("ChessGameActivity", "Castling applied successfully");
            } else {
                applyMoveDirectly(move);
            }
        } catch (Exception e) {
            Log.e("ChessGameActivity", "Error applying castling: " + e.getMessage());
            applyMoveDirectly(move);
        }
    }

    private void completeMoveApplication() {
        updateBoard();

        if (isTimedGame && chessTimer != null) {
            chessTimer.switchTurn();
        }

        updatePlayerTurn();

        updateOnlineTurn();

        if (chessBoard.isCheckmate(!chessBoard.isWhiteTurn())) {
            showGameOverDialog(chessBoard.isWhiteTurn());
        }

        Log.d("ChessGameActivity", "Opponent move applied successfully");
    }

    private void syncBoardState(boolean isWhiteTurn) {
        runOnUiThread(() -> {
            Log.d("ChessGameActivity", "Syncing board state - Server turn: " + isWhiteTurn +
                    ", Local turn: " + chessBoard.isWhiteTurn());

            if (chessBoard.isWhiteTurn() != isWhiteTurn) {
                chessBoard.setWhiteTurn(isWhiteTurn);
                Log.d("ChessGameActivity", "Turn synchronized to: " + (isWhiteTurn ? "White" : "Black"));
            }

            determineTurn();

            updatePlayerTurn();
        });
    }

    private void syncBoardWithServer() {
        Log.d("ChessGameActivity", "Syncing board with server state");

        chessBoard = new ChessBoard();
        updateBoard();

        FirebaseManager.getInstance().getGameSession(sessionId, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean isWhiteTurn = dataSnapshot.child("isWhiteTurn").getValue(Boolean.class);
                    if (isWhiteTurn != null) {
                        syncBoardState(isWhiteTurn);
                    }

                    applyAllMovesFromHistory(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ChessGameActivity", "Failed to sync board: " + databaseError.getMessage());
            }
        });
    }

    private void applyAllMovesFromHistory(DataSnapshot sessionSnapshot) {
        DataSnapshot movesSnapshot = sessionSnapshot.child("moves");
        for (DataSnapshot moveSnapshot : movesSnapshot.getChildren()) {
            ChessMove move = moveSnapshot.getValue(ChessMove.class);
            if (move != null && !move.getPlayerId().equals(currentUserId)) {
                applyMoveDirectly(move);
            }
        }
    }

    private void syncTurnWithServer() {
        if (!isOnlineGame) return;

        FirebaseManager.getInstance().getGameSession(sessionId, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean serverWhiteTurn = dataSnapshot.child("isWhiteTurn").getValue(Boolean.class);
                    if (serverWhiteTurn != null && chessBoard.isWhiteTurn() != serverWhiteTurn) {
                        runOnUiThread(() -> {
                            chessBoard.setWhiteTurn(serverWhiteTurn);
                            determineTurn();
                            updatePlayerTurn();
                            updateBoard();
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ChessGameActivity", "Failed to sync turn: " + databaseError.getMessage());
            }
        });
    }


    private void updateOnlineTurn() {
        isMyTurn = !isMyTurn;
        updateTurnIndicator();
    }

    private boolean hasSelectedPiece() {
        return chessBoard != null && chessBoard.getSelectedPiece() != null;
    }

    private ChessPiece getSelectedPieceSafe() {
        return chessBoard != null ? chessBoard.getSelectedPiece() : null;
    }

    private int getSelectedPieceRow() {
        ChessPiece selected = getSelectedPieceSafe();
        return selected != null ? selected.getRow() : -1;
    }

    private int getSelectedPieceCol() {
        ChessPiece selected = getSelectedPieceSafe();
        return selected != null ? selected.getCol() : -1;
    }

    private void restartGame() {
        chessBoard = new ChessBoard();
        movesCount = 0;
        gameStartTime = System.currentTimeMillis();
        if (isTimedGame) {
            if (chessTimer != null) {
                chessTimer.reset(gameTimeSeconds * 1000);
                chessTimer.start();
            } else {
                setupTimer();
            }
        }
        if (playerKingType != null) {
            chessBoard.activateKingAbility(playerKingType);
        }
        setupChessBoard();
        updatePlayerTurn();
        hidePromotionDialog();
        Toast.makeText(this, getString(R.string.game_restarted), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isTimedGame && chessTimer != null) {
            chessTimer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isTimedGame && chessTimer != null) {
            chessTimer.resume();
        }

        if (isOnlineGame) {
            syncKingDisplay();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isOnlineGame) {
            if (movesListener != null) {
                FirebaseManager.getInstance().stopListeningForMoves(sessionId, movesListener);
            }
            if (boardStateListener != null) {
                FirebaseManager.getInstance().stopListeningForBoardState(sessionId, boardStateListener);
            }
            if (abilityListener != null) {
                FirebaseManager.getInstance().stopListeningForAbilityActivations(sessionId, abilityListener);
            }
            if (transformationListener != null) {
                FirebaseManager.getInstance().stopListeningForPieceTransformations(sessionId, transformationListener);
            }
            if (dragonFireListener != null) {
                FirebaseManager.getInstance().stopListeningForDragonFireShots(sessionId, dragonFireListener);
            }
            if (playerExitListener != null) {
                FirebaseManager.getInstance().stopListeningForPlayerExit(sessionId, playerExitListener);
            }
        }

        if (isOnlineGame && !isFinishing()) {
            handlePlayerExit();
        }

        if (isTimedGame && chessTimer != null) {
            chessTimer.stop();
        }
    }

    private void handlePlayerExit() {
        try {
            Log.d("ChessGameActivity", "Player exiting online game - Session: " + sessionId);

            sendPlayerLeftNotification();

            FirebaseManager.getInstance().updateGameSessionStatus(
                    sessionId,
                    "abandoned",
                    currentUserId,
                    movesCount
            );

            saveGameResult(false, (int) ((System.currentTimeMillis() - gameStartTime) / 1000));

            new Handler().postDelayed(() -> {
                FirebaseManager.getInstance().deleteGameSession(sessionId);
            }, 10000);

        } catch (Exception e) {
            Log.e("ChessGameActivity", "Error handling player exit: " + e.getMessage());
        }
    }

    private void sendPlayerLeftNotification() {
        try {
            Map<String, Object> exitData = new HashMap<>();
            exitData.put("playerId", currentUserId);
            exitData.put("timestamp", System.currentTimeMillis());
            exitData.put("action", "player_left");
            exitData.put("playerName", getCurrentPlayerName()); // ✅ ДОБАВЛЯЕМ ИМЯ ИГРОКА

            FirebaseManager.getInstance().sendPlayerExitNotification(sessionId, exitData);

            Log.d("ChessGameActivity", "Exit notification sent for player: " + currentUserId);
        } catch (Exception e) {
            Log.e("ChessGameActivity", "Error sending exit notification: " + e.getMessage());
        }
    }

    private String getCurrentPlayerName() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("currentUserId", -1);
        User currentUser = dbHelper.getUser(userId);
        return currentUser != null ? currentUser.getUsername() : "Игрок";
    }

    private void showExitConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выход из игры")
                .setMessage("Вы уверены, что хотите выйти? Это засчитается как поражение.")
                .setPositiveButton("Выйти", (dialog, which) -> {
                    handlePlayerExit();
                    finish();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void setupPlayerExitListener() {
        playerExitListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (dataSnapshot.exists()) {
                    Map<String, Object> exitData = (Map<String, Object>) dataSnapshot.getValue();
                    if (exitData != null) {
                        String playerId = (String) exitData.get("playerId");
                        String action = (String) exitData.get("action");
                        String playerName = (String) exitData.get("playerName");

                        if (playerId != null && !playerId.equals(currentUserId) && "player_left".equals(action)) {
                            runOnUiThread(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    showOpponentLeftDialog(playerName);
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ChessGameActivity", "Exit listener cancelled: " + databaseError.getMessage());
            }
        };

        FirebaseManager.getInstance().listenForPlayerExit(sessionId, playerExitListener);
    }

    private void showOpponentLeftDialog(String opponentName) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        try {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Противник вышел")
                    .setMessage("Игрок " + opponentName + " покинул игру. Вам засчитана победа!")
                    .setPositiveButton("OK", (dialog, which) -> {
                        saveGameResult(true, (int) ((System.currentTimeMillis() - gameStartTime) / 1000));

                        FirebaseManager.getInstance().clearPlayerExitNotification(sessionId);

                        finish();
                    })
                    .setCancelable(false)
                    .setOnDismissListener(dialog -> {
                        FirebaseManager.getInstance().clearPlayerExitNotification(sessionId);
                    })
                    .show();
        } catch (Exception e) {
            Log.e("ChessGameActivity", "Error showing opponent left dialog: " + e.getMessage());
        }
    }

    private class AbilityListener implements ChildEventListener {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
            try {
                Map<String, Object> abilityData = (Map<String, Object>) dataSnapshot.getValue();
                if (abilityData != null) {
                    String playerId = (String) abilityData.get("playerId");
                    String kingType = (String) abilityData.get("kingType");
                    Boolean isWhiteTurn = (Boolean) abilityData.get("isWhiteTurn");

                    if (playerId != null && !playerId.equals(currentUserId) && isWhiteTurn != null) {
                        applyOpponentAbilityActivation(kingType, isWhiteTurn);
                    }
                }
            } catch (Exception e) {
                Log.e("ChessGameActivity", "Error processing ability activation", e);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {}

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {}

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.e("ChessGameActivity", "Ability listener cancelled: " + databaseError.getMessage());
        }
    }

    private class TransformationListener implements ChildEventListener {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
            try {
                Map<String, Object> transformData = (Map<String, Object>) dataSnapshot.getValue();
                if (transformData != null) {
                    String playerId = (String) transformData.get("playerId");
                    Long rowLong = (Long) transformData.get("row");
                    Long colLong = (Long) transformData.get("col");
                    String newPieceType = (String) transformData.get("newPieceType");
                    Boolean changeColor = (Boolean) transformData.get("changeColor");

                    if (playerId != null && !playerId.equals(currentUserId) &&
                            rowLong != null && colLong != null) {
                        applyOpponentPieceTransformation(
                                rowLong.intValue(),
                                colLong.intValue(),
                                newPieceType,
                                changeColor != null ? changeColor : false
                        );
                    }
                }
            } catch (Exception e) {
                Log.e("ChessGameActivity", "Error processing piece transformation", e);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {}

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {}

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.e("ChessGameActivity", "Transformation listener cancelled: " + databaseError.getMessage());
        }
    }

    private void checkForCheckmate() {
        if (isGameFinished) return;

        new Handler().postDelayed(() -> {
            if (!isGameFinished && chessBoard.isCheckmate(!chessBoard.isWhiteTurn())) {
                showGameOverDialog(chessBoard.isWhiteTurn());
            }
        }, 100);
    }

}