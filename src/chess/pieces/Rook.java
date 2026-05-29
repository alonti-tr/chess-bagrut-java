package chess.pieces;

import chess.Board;
import chess.Color;
import java.util.List;

public class Rook extends Piece {

    private static final int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}};

    public Rook(Color color) { super(color); }

    @Override public String getSymbol() { return "R"; }
    @Override public int getValue()     { return 5; }

    @Override
    public List<int[]> getMoves(Board board, int col, int row) {
        return slideMoves(board, col, row, DIRS);
    }
}
