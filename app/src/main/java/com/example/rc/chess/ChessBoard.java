package com.example.rc.chess;

import com.example.rc.chess.pieces.Pawn;
import com.example.rc.chess.pieces.Rook;
import com.example.rc.chess.pieces.Knight;
import com.example.rc.chess.pieces.Bishop;
import com.example.rc.chess.pieces.Queen;
import com.example.rc.chess.pieces.King;

import java.util.ArrayList;
import java.util.List;

public class ChessBoard {
    private ChessPiece[][] board;
    private boolean isWhiteTurn;
    private ChessPiece selectedPiece;
    private List<int[]> possibleMoves;
    private boolean gnomeAbilityActive = false;
    private boolean elfAbilityUsedWhite = false;
    private boolean elfAbilityUsedBlack = false;
    private int elfAbilityTargetRow = -1;
    private int elfAbilityTargetCol = -1;
    private boolean humanAbilityUsedWhite = false;
    private boolean humanAbilityUsedBlack = false;
    private int humanAbilityRemainingUsesWhite = 2;
    private int humanAbilityRemainingUsesBlack = 2;
    private boolean dragonFireShotUsedWhite = false;
    private boolean dragonFireShotUsedBlack = false;
    private String activeKingAbility = null;

    public ChessBoard() {
        board = new ChessPiece[8][8];
        isWhiteTurn = true;
        possibleMoves = new ArrayList<>();
        initializeBoard();
    }

    private void initializeBoard() {
        board[0][0] = new Rook(false, 0, 0);
        board[0][1] = new Knight(false, 0, 1);
        board[0][2] = new Bishop(false, 0, 2);
        board[0][3] = new Queen(false, 0, 3);
        board[0][4] = new King(false, 0, 4);
        board[0][5] = new Bishop(false, 0, 5);
        board[0][6] = new Knight(false, 0, 6);
        board[0][7] = new Rook(false, 0, 7);

        for (int i = 0; i < 8; i++) {
            board[1][i] = new Pawn(false, 1, i);
        }

        board[7][0] = new Rook(true, 7, 0);
        board[7][1] = new Knight(true, 7, 1);
        board[7][2] = new Bishop(true, 7, 2);
        board[7][3] = new Queen(true, 7, 3);
        board[7][4] = new King(true, 7, 4);
        board[7][5] = new Bishop(true, 7, 5);
        board[7][6] = new Knight(true, 7, 6);
        board[7][7] = new Rook(true, 7, 7);

        for (int i = 0; i < 8; i++) {
            board[6][i] = new Pawn(true, 6, i);
        }
    }

    public ChessPiece getPiece(int row, int col) {
        if (row < 0 || row >= 8 || col < 0 || col >= 8) {
            return null;
        }
        return board[row][col];
    }

    public boolean selectPiece(int row, int col) {
        ChessPiece piece = getPiece(row, col);
        if (piece != null && piece.isWhite() == isWhiteTurn) {
            selectedPiece = piece;
            calculatePossibleMoves();
            return true;
        }
        return false;
    }

    public boolean movePiece(int toRow, int toCol) {
        if (selectedPiece == null || !isValidMove(toRow, toCol)) {
            return false;
        }

        if (!isMoveSafeFromCheck(selectedPiece, toRow, toCol)) {
            return false;
        }

        if (selectedPiece.getType() == ChessPiece.PieceType.KING && Math.abs(toCol - selectedPiece.getCol()) == 2) {
            performCastling(toRow, toCol);
        } else {
            board[selectedPiece.getRow()][selectedPiece.getCol()] = null;
            board[toRow][toCol] = selectedPiece;
            selectedPiece.setPosition(toRow, toCol);

            if (selectedPiece.getType() == ChessPiece.PieceType.KING) {
                ((King) selectedPiece).setMoved();
            } else if (selectedPiece.getType() == ChessPiece.PieceType.ROOK) {
                ((Rook) selectedPiece).setMoved();
            }
        }
        isWhiteTurn = !isWhiteTurn;
        selectedPiece = null;
        possibleMoves.clear();

        return true;
    }

    private void performCastling(int toRow, int toCol) {
        King king = (King) selectedPiece;
        int direction = toCol > king.getCol() ? 1 : -1;
        int rookCol = direction == 1 ? 7 : 0;
        int newRookCol = direction == 1 ? toCol - 1 : toCol + 1;

        board[king.getRow()][king.getCol()] = null;
        board[toRow][toCol] = king;
        king.setPosition(toRow, toCol);
        king.setMoved();

        Rook rook = (Rook) board[toRow][rookCol];
        board[toRow][rookCol] = null;
        board[toRow][newRookCol] = rook;
        rook.setPosition(toRow, newRookCol);
        rook.setMoved();
    }

    private void calculatePossibleMoves() {
        possibleMoves.clear();
        if (selectedPiece == null) return;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (selectedPiece.isValidMove(row, col, this)) {
                    if (isMoveSafeFromCheck(selectedPiece, row, col)) {
                        possibleMoves.add(new int[]{row, col});
                    }
                }
            }
        }
    }

    private boolean isValidMove(int toRow, int toCol) {
        for (int[] move : possibleMoves) {
            if (move[0] == toRow && move[1] == toCol) {
                return true;
            }
        }
        return false;
    }

    public void promotePawn(int row, int col, int pieceType) {
        ChessPiece pawn = getPiece(row, col);
        if (pawn == null || pawn.getType() != ChessPiece.PieceType.PAWN) {
            return;
        }

        boolean isWhite = pawn.isWhite();
        ChessPiece newPiece;

        switch (pieceType) {
            case 1: // Ферзь
                newPiece = new Queen(isWhite, row, col);
                break;
            case 2: // Ладья
                newPiece = new Rook(isWhite, row, col);
                break;
            case 3: // Слон
                newPiece = new Bishop(isWhite, row, col);
                break;
            case 4: // Конь
                newPiece = new Knight(isWhite, row, col);
                break;
            default:
                newPiece = new Queen(isWhite, row, col);
        }

        board[row][col] = newPiece;
    }

    public boolean isKingInCheck(boolean isWhiteKing) {
        King king = findKing(isWhiteKing);
        if (king == null) return false;

        return isSquareUnderAttack(king.getRow(), king.getCol(), !isWhiteKing);
    }

    private King findKing(boolean isWhite) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board[row][col];
                if (piece != null &&
                        piece.getType() == ChessPiece.PieceType.KING &&
                        piece.isWhite() == isWhite) {
                    return (King) piece;
                }
            }
        }
        return null;
    }

    private boolean isSquareUnderAttack(int targetRow, int targetCol, boolean byWhite) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board[row][col];
                if (piece != null && piece.isWhite() == byWhite) {
                    int originalRow = piece.getRow();
                    int originalCol = piece.getCol();

                    if (piece.isValidMove(targetRow, targetCol, this)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isCheckmate(boolean isWhiteKing) {
        if (!isKingInCheck(isWhiteKing)) {
            return false;
        }

        return !hasAnyValidMove(isWhiteKing);
    }

    private boolean hasAnyValidMove(boolean isWhite) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board[row][col];
                if (piece != null && piece.isWhite() == isWhite) {
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            if (piece.isValidMove(toRow, toCol, this)) {
                                if (isMoveSafeFromCheck(piece, toRow, toCol)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isMoveSafeFromCheck(ChessPiece piece, int toRow, int toCol) {
        ChessPiece originalPiece = board[piece.getRow()][piece.getCol()];
        ChessPiece targetPiece = board[toRow][toCol];

        board[piece.getRow()][piece.getCol()] = null;
        board[toRow][toCol] = piece;
        int originalRow = piece.getRow();
        int originalCol = piece.getCol();
        piece.setPosition(toRow, toCol);

        boolean stillInCheck = isKingInCheck(piece.isWhite());

        board[originalRow][originalCol] = originalPiece;
        board[toRow][toCol] = targetPiece;
        piece.setPosition(originalRow, originalCol);

        return !stillInCheck;
    }

    public ChessPiece getSelectedPiece() { return selectedPiece; }
    public List<int[]> getPossibleMoves() {
        return possibleMoves;
    }
    public boolean isWhiteTurn() { return isWhiteTurn; }

    public boolean isGnomeAbilityActive() {
        return gnomeAbilityActive;
    }

    public String getActiveKingAbility() {
        return activeKingAbility;
    }

    public void activateKingAbility(String kingType) {
        this.activeKingAbility = kingType;

        switch (kingType) {
            case "gnome":
                break;
            case "elf":
                break;
            case "human":
                break;
            case "dragon":
                break;
        }
    }

    public boolean activateElfAbility(int pawnRow, int pawnCol) {
        if (!"elf".equals(activeKingAbility)) {
            return false;
        }

        ChessPiece pawn = getPiece(pawnRow, pawnCol);
        if (pawn == null || pawn.getType() != ChessPiece.PieceType.PAWN) {
            return false;
        }

        if ((pawn.isWhite() && elfAbilityUsedWhite) ||
                (!pawn.isWhite() && elfAbilityUsedBlack)) {
            return false;
        }

        board[pawnRow][pawnCol] = new Knight(pawn.isWhite(), pawnRow, pawnCol);

        if (pawn.isWhite()) {
            elfAbilityUsedWhite = true;
        } else {
            elfAbilityUsedBlack = true;
        }

        return true;
    }

    public boolean isElfAbilityAvailable() {
        if (!"elf".equals(activeKingAbility)) {
            return false;
        }

        if (isWhiteTurn && !elfAbilityUsedWhite) {
            return true;
        } else if (!isWhiteTurn && !elfAbilityUsedBlack) {
            return true;
        }

        return false;
    }

    public boolean activateHumanAbility(int enemyPawnRow, int enemyPawnCol) {
        if (!"human".equals(activeKingAbility)) {
            return false;
        }

        ChessPiece enemyPawn = getPiece(enemyPawnRow, enemyPawnCol);
        if (enemyPawn == null ||
                enemyPawn.getType() != ChessPiece.PieceType.PAWN ||
                enemyPawn.isWhite() == isWhiteTurn ||
                !isOnOurHalf(enemyPawnRow, isWhiteTurn)) {
            return false;
        }

        if ((isWhiteTurn && (humanAbilityUsedWhite || humanAbilityRemainingUsesWhite <= 0)) ||
                (!isWhiteTurn && (humanAbilityUsedBlack || humanAbilityRemainingUsesBlack <= 0))) {
            return false;
        }

        board[enemyPawnRow][enemyPawnCol] = new Pawn(isWhiteTurn, enemyPawnRow, enemyPawnCol);

        if (isWhiteTurn) {
            humanAbilityRemainingUsesWhite--;
            if (humanAbilityRemainingUsesWhite <= 0) {
                humanAbilityUsedWhite = true;
            }
        } else {
            humanAbilityRemainingUsesBlack--;
            if (humanAbilityRemainingUsesBlack <= 0) {
                humanAbilityUsedBlack = true;
            }
        }

        return true;
    }

    private boolean isOnOurHalf(int row, boolean isWhite) {
        return isWhite ? row >= 4 : row <= 3;
    }

    public boolean isHumanAbilityAvailable() {
        if (!"human".equals(activeKingAbility)) {
            return false;
        }

        if (isWhiteTurn && !humanAbilityUsedWhite && humanAbilityRemainingUsesWhite > 0) {
            return true;
        } else if (!isWhiteTurn && !humanAbilityUsedBlack && humanAbilityRemainingUsesBlack > 0) {
            return true;
        }

        return false;
    }

    public int getHumanAbilityRemainingUses() {
        if (isWhiteTurn) {
            return humanAbilityRemainingUsesWhite;
        } else {
            return humanAbilityRemainingUsesBlack;
        }
    }

    public boolean activateDragonFireShot(int fromRow, int fromCol, int toRow, int toCol) {
        if (!"dragon".equals(activeKingAbility)) {
            return false;
        }

        if ((isWhiteTurn && dragonFireShotUsedWhite) || (!isWhiteTurn && dragonFireShotUsedBlack)) {
            return false;
        }

        ChessPiece targetPiece = getPiece(toRow, toCol);
        if (targetPiece != null && targetPiece.getType() == ChessPiece.PieceType.KING) {
            return false;
        }

        ChessPiece shooter = getPiece(fromRow, fromCol);
        if (shooter == null || shooter.getType() != ChessPiece.PieceType.PAWN) {
            return false;
        }

        if (fromRow != toRow && fromCol != toCol) {
            return false;
        }

        if (!isPathClearForFire(fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        board[toRow][toCol] = null;

        if (isWhiteTurn) {
            dragonFireShotUsedWhite = true;
        } else {
            dragonFireShotUsedBlack = true;
        }

        return true;
    }

    private boolean isPathClearForFire(int fromRow, int fromCol, int toRow, int toCol) {
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);

        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;

        while (currentRow != toRow || currentCol != toCol) {
            if (getPiece(currentRow, currentCol) != null) {
                return false;
            }
            currentRow += rowStep;
            currentCol += colStep;
        }
        return true;
    }

    public boolean isDragonFireShotAvailable() {
        if (!"dragon".equals(activeKingAbility)) {
            return false;
        }

        boolean available;
        if (isWhiteTurn) {
            available = !dragonFireShotUsedWhite;
        } else {
            available = !dragonFireShotUsedBlack;
        }

        return available;
    }

}