package chess;

import chess.pieces.*;
import java.util.ArrayList;
import java.util.List;

public class Board {

    private Piece[][] grid = new Piece[8][8];
    public Color turn = Color.WHITE;
    public String status = "playing";
    public Color winner = null;
    public int[] lastMove = null;
    public int[] enPassant = null;

    public Board() {
        setupPieces(Color.WHITE, 0);
        setupPieces(Color.BLACK, 7);
        for (int c = 0; c < 8; c++) {
            grid[c][1] = new Pawn(Color.WHITE);
            grid[c][6] = new Pawn(Color.BLACK);
        }
    }

    private void setupPieces(Color color, int row) {
        grid[0][row] = new Rook(color);
        grid[1][row] = new Knight(color);
        grid[2][row] = new Bishop(color);
        grid[3][row] = new Queen(color);
        grid[4][row] = new King(color);
        grid[5][row] = new Bishop(color);
        grid[6][row] = new Knight(color);
        grid[7][row] = new Rook(color);
    }

    public Piece getPiece(int col, int row) {
        return grid[col][row];
    }

    public boolean applyMove(int fc, int fr, int tc, int tr) {
        return applyMove(fc, fr, tc, tr, "Q");
    }

    public boolean isLegalMove(int fc, int fr, int tc, int tr) {
        for (int[] m : legalMoves(fc, fr)) {
            if (m[0] == tc && m[1] == tr) return true;
        }
        return false;
    }

    public boolean isPromotionMove(int fc, int fr, int tc, int tr) {
        Piece piece = grid[fc][fr];
        if (!(piece instanceof Pawn)) return false;
        int promoRow = (piece.color == Color.WHITE) ? 7 : 0;
        return tr == promoRow;
    }

    public boolean applyMove(int fc, int fr, int tc, int tr, String promotion) {
        if (!isLegalMove(fc, fr, tc, tr)) return false;

        Piece piece = grid[fc][fr];

        if (piece instanceof Pawn && fc != tc && grid[tc][tr] == null) {
            grid[tc][fr] = null;
        }

        grid[tc][tr] = piece;
        grid[fc][fr] = null;
        piece.hasMoved = true;
        lastMove = new int[]{fc, fr, tc, tr};

        if (piece instanceof King && Math.abs(tc - fc) == 2) {
            if (tc == 6) {
                grid[5][fr] = grid[7][fr];
                grid[7][fr] = null;
            } else {
                grid[3][fr] = grid[0][fr];
                grid[0][fr] = null;
            }
            if (grid[tc == 6 ? 5 : 3][fr] != null)
                grid[tc == 6 ? 5 : 3][fr].hasMoved = true;
        }

        enPassant = null;
        if (piece instanceof Pawn && Math.abs(tr - fr) == 2) {
            enPassant = new int[]{fc, (fr + tr) / 2};
        }

        if (piece instanceof Pawn) {
            int promoRow = (piece.color == Color.WHITE) ? 7 : 0;
            if (tr == promoRow) grid[tc][tr] = createPromotion(promotion, piece.color);
        }

        turn = turn.opposite();
        updateStatus();
        return true;
    }

    private Piece createPromotion(String choice, Color color) {
        if (choice == null) return new Queen(color);
        switch (choice) {
            case "R": return new Rook(color);
            case "B": return new Bishop(color);
            case "N": return new Knight(color);
            default:  return new Queen(color);
        }
    }

    public List<int[]> legalMoves(int col, int row) {
        Piece piece = grid[col][row];
        if (piece == null || piece.color != turn) return new ArrayList<>();
        List<int[]> raw = piece.getMoves(this, col, row);
        List<int[]> legal = new ArrayList<>();
        for (int[] m : raw) {
            if (!moveLeavesKingInCheck(col, row, m[0], m[1])) legal.add(m);
        }
        if (piece instanceof King) addCastlingMoves(legal, col, row, piece.color);
        return legal;
    }

    private void addCastlingMoves(List<int[]> legal, int kc, int kr, Color color) {
        if (grid[kc][kr].hasMoved) return;
        if (isInCheck(color)) return;
        Color opp = color.opposite();

        Piece kRook = grid[7][kr];
        if (kRook instanceof Rook && !kRook.hasMoved
                && grid[5][kr] == null && grid[6][kr] == null
                && !isAttackedSimple(5, kr, opp) && !isAttackedSimple(6, kr, opp)) {
            legal.add(new int[]{6, kr});
        }

        Piece qRook = grid[0][kr];
        if (qRook instanceof Rook && !qRook.hasMoved
                && grid[1][kr] == null && grid[2][kr] == null && grid[3][kr] == null
                && !isAttackedSimple(2, kr, opp) && !isAttackedSimple(3, kr, opp)) {
            legal.add(new int[]{2, kr});
        }
    }

    private boolean isAttackedSimple(int col, int row, Color by) {
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                Piece p = grid[c][r];
                if (p == null || p.color != by) continue;
                for (int[] m : p.getMoves(this, c, r)) {
                    if (m[0] == col && m[1] == row) return true;
                }
            }
        }
        return false;
    }

    private boolean moveLeavesKingInCheck(int fc, int fr, int tc, int tr) {
        Piece saved = grid[tc][tr];
        grid[tc][tr] = grid[fc][fr];
        grid[fc][fr] = null;
        boolean inCheck = isInCheck(turn);
        grid[fc][fr] = grid[tc][tr];
        grid[tc][tr] = saved;
        return inCheck;
    }

    public boolean isInCheck(Color color) {
        int kc = -1, kr = -1;
        outer:
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                if (grid[c][r] instanceof King && grid[c][r].color == color) {
                    kc = c; kr = r; break outer;
                }
            }
        }
        if (kc < 0) return true;

        Color opp = color.opposite();
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                Piece p = grid[c][r];
                if (p == null || p.color != opp) continue;
                for (int[] m : p.getMoves(this, c, r)) {
                    if (m[0] == kc && m[1] == kr) return true;
                }
            }
        }
        return false;
    }

    private boolean hasAnyLegalMove(Color color) {
        Color saved = turn;
        turn = color;
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                if (grid[c][r] != null && grid[c][r].color == color) {
                    if (!legalMoves(c, r).isEmpty()) { turn = saved; return true; }
                }
            }
        }
        turn = saved;
        return false;
    }

    private void updateStatus() {
        if (!hasAnyLegalMove(turn)) {
            if (isInCheck(turn)) {
                status = "checkmate";
                winner = turn.opposite();
            } else {
                status = "stalemate";
            }
        }
    }

    public String[][] toSimple() {
        String[][] s = new String[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                s[r][c] = (grid[c][r] == null) ? "." : grid[c][r].code();
            }
        }
        return s;
    }
}
