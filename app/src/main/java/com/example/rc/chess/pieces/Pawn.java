package com.example.rc.chess.pieces;

import com.example.rc.chess.ChessBoard;
import com.example.rc.chess.ChessPiece;

public class Pawn extends ChessPiece {

    public Pawn(boolean isWhite, int row, int col) {
        super(isWhite, row, col, PieceType.PAWN);
    }

    @Override
    public boolean isValidMove(int toRow, int toCol, ChessBoard board) {
        if (toRow < 0 || toRow >= 8 || toCol < 0 || toCol >= 8) {
            return false;
        }

        int direction = isWhite ? -1 : 1;
        int startRow = isWhite ? 6 : 1;

        // Обычные ходы вперед
        if (toCol == col) {
            // Ход на одну клетку вперед
            if (toRow == row + direction && board.getPiece(toRow, toCol) == null) {
                return true;
            }
            // Ход на две клетки вперед с начальной позиции
            if (row == startRow && toRow == row + 2 * direction &&
                    board.getPiece(toRow, toCol) == null &&
                    board.getPiece(row + direction, col) == null) {
                return true;
            }
        }

        // Взятие по диагонали вперед
        if (Math.abs(toCol - col) == 1 && toRow == row + direction) {
            ChessPiece target = board.getPiece(toRow, toCol);
            if (target != null && target.isWhite() != isWhite) {
                return true;
            }
        }

        // СПОСОБНОСТЬ ГНОМОВ: ходы назад
        if (board.getActiveKingAbility() != null &&
                board.getActiveKingAbility().equals("gnome") &&
                isWhite == board.isWhiteTurn()) {

            // Ход на одну клетку назад
            if (toCol == col && toRow == row - direction &&
                    board.getPiece(toRow, toCol) == null) {
                return true;
            }

            // Взятие назад по диагонали
            if (Math.abs(toCol - col) == 1 && toRow == row - direction) {
                ChessPiece target = board.getPiece(toRow, toCol);
                if (target != null && target.isWhite() != isWhite) {
                    return true;
                }
            }
        }

        return false;
    }
}