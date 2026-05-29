package chess.pieces;

import chess.Board;
import chess.Color;
import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {

    public Pawn(Color color) { super(color); }

    @Override public String getSymbol() { return "P"; }
    @Override public int getValue()     { return 1; }

    @Override
    public List<int[]> getMoves(Board board, int col, int row) {
        List<int[]> moves = new ArrayList<>();
        int dir = (color == Color.WHITE) ? 1 : -1;
        int startRow = (color == Color.WHITE) ? 1 : 6;
        int promoRow = (color == Color.WHITE) ? 7 : 0;

        int oneStep = row + dir;
        if (inBounds(col, oneStep) && board.getPiece(col, oneStep) == null) {
            moves.add(new int[]{col, oneStep});
            int twoStep = row + 2 * dir;
            if (row == startRow && board.getPiece(col, twoStep) == null)
                moves.add(new int[]{col, twoStep});
        }

        for (int dc : new int[]{-1, 1}) {
            int tc = col + dc, tr = oneStep;
            if (!inBounds(tc, tr)) continue;
            Piece target = board.getPiece(tc, tr);
            if (target != null && target.color != color) {
                moves.add(new int[]{tc, tr});
            } else if (target == null && board.enPassant != null
                    && board.enPassant[0] == tc && board.enPassant[1] == tr) {
                moves.add(new int[]{tc, tr});
            }
        }
        return moves;
    }
}
