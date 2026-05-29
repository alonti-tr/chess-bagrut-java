package chess.pieces;

import chess.Board;
import chess.Color;
import java.util.ArrayList;
import java.util.List;

public abstract class Piece {

    public final Color color;
    public boolean hasMoved;

    public Piece(Color color) {
        this.color = color;
        this.hasMoved = false;
    }

    public abstract List<int[]> getMoves(Board board, int col, int row);
    public abstract int getValue();
    public abstract String getSymbol();

    public String code() {
        return (color == Color.WHITE ? "w" : "b") + getSymbol();
    }

    protected boolean inBounds(int col, int row) {
        return col >= 0 && col < 8 && row >= 0 && row < 8;
    }

    protected List<int[]> slideMoves(Board board, int col, int row, int[][] dirs) {
        List<int[]> moves = new ArrayList<>();
        for (int[] d : dirs) {
            int step = 1;
            while (true) {
                int tc = col + d[0] * step;
                int tr = row + d[1] * step;
                if (!inBounds(tc, tr)) break;
                Piece target = board.getPiece(tc, tr);
                if (target == null) {
                    moves.add(new int[]{tc, tr});
                } else {
                    if (target.color != color) moves.add(new int[]{tc, tr});
                    break;
                }
                step++;
            }
        }
        return moves;
    }

    protected List<int[]> jumpMoves(Board board, int col, int row, int[][] offsets) {
        List<int[]> moves = new ArrayList<>();
        for (int[] off : offsets) {
            int tc = col + off[0];
            int tr = row + off[1];
            if (!inBounds(tc, tr)) continue;
            Piece target = board.getPiece(tc, tr);
            if (target == null || target.color != color) moves.add(new int[]{tc, tr});
        }
        return moves;
    }
}
