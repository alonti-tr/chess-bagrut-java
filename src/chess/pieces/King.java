package chess.pieces;

import chess.Board;
import chess.Color;
import java.util.List;

public class King extends Piece {

    private static final int[][] OFFSETS = {
        {1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1}
    };

    public King(Color color) { super(color); }

    @Override public String getSymbol() { return "K"; }
    @Override public int getValue()     { return 100; }

    @Override
    public List<int[]> getMoves(Board board, int col, int row) {
        return jumpMoves(board, col, row, OFFSETS);
    }
}
