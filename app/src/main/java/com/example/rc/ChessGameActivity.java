package com.example.rc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rc.adapters.PromotionAdapter;
import com.example.rc.chess.ChessBoard;
import com.example.rc.chess.ChessPiece;
import com.example.rc.chess.ChessTimer;
import com.example.rc.database.DatabaseHelper;
import com.example.rc.models.GameStat;
import com.example.rc.models.User;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chess_game);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isPlayerWhite = extras.getBoolean("player_color_white", true);
            gameTimeSeconds = extras.getInt("game_time_seconds", 600);
            isTimedGame = extras.getBoolean("is_timed_game", true);
        }

        gameStartTime = System.currentTimeMillis();
        movesCount = 0;

        initViews();
        setupChessBoard();

        if (isTimedGame) {
            setupTimer();
        } else {
            findViewById(R.id.timerWhiteLayout).setVisibility(View.GONE);
            findViewById(R.id.timerBlackLayout).setVisibility(View.GONE);
        }

        setupAdapters();
        updatePlayerTurn();
    }

    private void initViews() {
        chessGrid = findViewById(R.id.chessGrid);
        tvCurrentPlayer = findViewById(R.id.tvCurrentPlayer);
        btnBack = findViewById(R.id.btnBack);
        btnRestart = findViewById(R.id.btnRestart);

        rvPromotion = findViewById(R.id.rvPromotion);
        promotionDialog = findViewById(R.id.promotionDialog);

        chessGrid.setColumnCount(8);
        chessGrid.setRowCount(8);

        btnBack.setOnClickListener(v -> finish());
        btnRestart.setOnClickListener(v -> restartGame());

        if (promotionDialog != null) {
            promotionDialog.setVisibility(View.GONE);
        }

        tvWhiteTimer = findViewById(R.id.tvWhiteTimer);
        tvBlackTimer = findViewById(R.id.tvBlackTimer);
        indicatorWhite = findViewById(R.id.indicatorWhite);
        indicatorBlack = findViewById(R.id.indicatorBlack);
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

        ChessPiece piece = chessBoard.getPiece(row, col);

        if (chessBoard.getSelectedPiece() != null) {
            if (chessBoard.movePiece(row, col)) {
                movesCount++;
                if (isTimedGame && chessTimer != null) {
                    chessTimer.switchTurn();
                }

                if (isPawnPromotion(row, col)) {
                    if (isTimedGame && chessTimer != null) {
                        chessTimer.pause();
                    }
                    showPromotionDialog(row, col);
                } else {
                    updateBoard();
                    updatePlayerTurn();

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
            hidePromotionDialog();

            if (chessBoard.isCheckmate(!chessBoard.isWhiteTurn())) {
                showGameOverDialog(chessBoard.isWhiteTurn());
            }
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
        if (isTimedGame && chessTimer != null) {
            chessTimer.stop();
        }

        int gameDuration = (int) ((System.currentTimeMillis() - gameStartTime) / 1000);

        saveGameResult(isWhiteWinner, gameDuration);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Игра окончена!")
                .setMessage("ШАХ И МАТ!\nПобедили: " + (isWhiteWinner ? "Белые" : "Черные"))
                .setPositiveButton("Новая игра", (dialog, which) -> restartGame())
                .setNegativeButton("Выход", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void saveGameResult(boolean isWhiteWinner, int durationSeconds) {

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        User currentUser = dbHelper.getCurrentUser();

        if (currentUser != null) {
            GameStat stat = new GameStat(
                    currentUser.getId(),
                    isPlayerWhite == isWhiteWinner ? "win" : "loss",
                    isPlayerWhite ? "white" : "black",
                    durationSeconds,
                    this.movesCount
            );


            dbHelper.addGameStat(stat);
        }
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isTimedGame && chessTimer != null) {
            chessTimer.stop();
        }
    }
}