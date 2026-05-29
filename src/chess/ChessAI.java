package chess;

import chess.pieces.King;
import chess.pieces.Piece;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChessAI {

    private final int level;
    private final Random rng = new Random();

    public ChessAI(int level) {
        this.level = level;
    }

    public int[] pickMove(Board board) {
        List<int[]> all = allMoves(board);
        if (all.isEmpty()) return null;
        if (level == 1) return all.get(rng.nextInt(all.size()));
        return greedyMove(board, all);
    }

    private int[] greedyMove(Board board, List<int[]> moves) {
        int bestScore = Integer.MIN_VALUE;
        List<int[]> bestMoves = new ArrayList<>();
        for (int[] m : moves) {
            int score = scoreMove(board, m);
            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(m);
            } else if (score == bestScore) {
                bestMoves.add(m);
            }
        }
        return bestMoves.get(rng.nextInt(bestMoves.size()));
    }

    private int scoreMove(Board board, int[] m) {
        int score = 0;
        Piece target = board.getPiece(m[2], m[3]);
        if (target != null) score += target.getValue() * 10;

        int dc = m[2] - 3;
        int dr = m[3] - 3;
        int centerDist = (int) Math.sqrt(dc * dc + dr * dr);
        score += Math.max(0, 4 - centerDist);

        return score;
    }

    private List<int[]> allMoves(Board board) {
        List<int[]> result = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                Piece p = board.getPiece(c, r);
                if (p == null || p.color != board.turn) continue;
                for (int[] to : board.legalMoves(c, r)) {
                    result.add(new int[]{c, r, to[0], to[1]});
                }
            }
        }
        return result;
    }
}
