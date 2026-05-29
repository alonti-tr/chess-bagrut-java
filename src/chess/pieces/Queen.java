package chess.pieces;

import chess.Board;
import chess.Color;
import java.util.List;

public class Queen extends Piece {

    private static final int[][] DIRS = {
        {1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}
    };

    public Queen(Color color) { super(color); }

    @Override public String getSymbol() { return "Q"; }
    @Override public int getValue()     { return 9; }

    @Override
    public List<int[]> getMoves(Board board, int col, int row) {
        return slideMoves(board, col, row, DIRS);
    }
}
