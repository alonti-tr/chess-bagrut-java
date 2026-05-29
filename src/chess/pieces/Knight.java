package chess.pieces;

import chess.Board;
import chess.Color;
import java.util.List;

public class Knight extends Piece {

    private static final int[][] OFFSETS = {
        {1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2}
    };

    public Knight(Color color) { super(color); }

    @Override public String getSymbol() { return "N"; }
    @Override public int getValue()     { return 3; }

    @Override
    public List<int[]> getMoves(Board board, int col, int row) {
        return jumpMoves(board, col, row, OFFSETS);
    }
}
