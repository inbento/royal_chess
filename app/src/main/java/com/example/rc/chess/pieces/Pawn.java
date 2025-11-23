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

        if (toCol == col) {
            if (toRow == row + direction && board.getPiece(toRow, toCol) == null) {
                return true;
            }
            if (row == startRow && toRow == row + 2 * direction &&
                    board.getPiece(toRow, toCol) == null &&
                    board.getPiece(row + direction, col) == null) {
                return true;
            }
        }

        if (Math.abs(toCol - col) == 1 && toRow == row + direction) {
            ChessPiece target = board.getPiece(toRow, toCol);
            if (target != null && target.isWhite() != isWhite) {
                return true;
            }
        }

        if (board.getActiveKingAbility() != null &&
                board.getActiveKingAbility().equals("gnome") &&
                isWhite == board.isWhiteTurn()) {

            if (toCol == col && toRow == row - direction &&
                    board.getPiece(toRow, toCol) == null) {
                return true;
            }

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