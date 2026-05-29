package chess.pieces;

import chess.Board;
import chess.Color;
import java.util.List;

public class Bishop extends Piece {

    private static final int[][] DIRS = {{1,1},{1,-1},{-1,1},{-1,-1}};

    public Bishop(Color color) { super(color); }

    @Override public String getSymbol() { return "B"; }
    @Override public int getValue()     { return 3; }

    @Override
    public List<int[]> getMoves(Board board, int col, int row) {
        return slideMoves(board, col, row, DIRS);
    }
}
