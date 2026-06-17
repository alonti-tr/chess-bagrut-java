package chess.server;

import chess.Board;
import chess.Color;
import chess.JSONUtil;
import chess.MoveProvider;
import java.util.LinkedHashMap;
import java.util.Map;

public class Game {

    private final Board board = new Board();
    private final ClientHandler white;
    private final ClientHandler black;
    private final MoveProvider ai;

    private int[] pendingMove = null;
    private ClientHandler pendingPlayer = null;

    public Game(ClientHandler white, ClientHandler black, MoveProvider ai) {
        this.white = white;
        this.black = black;
        this.ai = ai;
    }

    public void start() {
        String opponentName = black == null ? ai.displayName() : black.getUsername();
        sendStart(white, Color.WHITE, opponentName);
        if (black != null) sendStart(black, Color.BLACK, white.getUsername());
        broadcastState();
    }

    public synchronized int[][] getLegalMoves(ClientHandler handler, int col, int row) {
        Color playerColor = (handler == white) ? Color.WHITE : Color.BLACK;
        if (board.turn != playerColor) return new int[0][];
        java.util.List<int[]> moves = board.legalMoves(col, row);
        return moves.toArray(new int[0][]);
    }

    public synchronized boolean applyMove(ClientHandler handler, int fc, int fr, int tc, int tr) {
        Color playerColor = (handler == white) ? Color.WHITE : Color.BLACK;
        if (board.turn != playerColor) return false;
        if (pendingPlayer != null) return false;

        if (!board.isLegalMove(fc, fr, tc, tr)) return false;

        if (board.isPromotionMove(fc, fr, tc, tr)) {
            pendingMove = new int[]{fc, fr, tc, tr};
            pendingPlayer = handler;
            handler.send(buildChoosePromotion());
            return true;
        }

        board.applyMove(fc, fr, tc, tr);
        afterMove();
        return true;
    }

    public synchronized void completePromotion(ClientHandler handler, String choice) {
        if (pendingPlayer != handler || pendingMove == null) return;
        int[] m = pendingMove;
        pendingMove = null;
        pendingPlayer = null;
        board.applyMove(m[0], m[1], m[2], m[3], choice);
        afterMove();
    }

    private void afterMove() {
        broadcastState();
        if (!"playing".equals(board.status)) return;

        if (ai != null && board.turn == Color.BLACK) {
            int[] move = ai.pickMove(board);
            if (move != null) {
                applyAiMove(move);
                broadcastState();
            }
        }
    }

    private void applyAiMove(int[] move) {
        if (board.isPromotionMove(move[0], move[1], move[2], move[3])) {
            board.applyMove(move[0], move[1], move[2], move[3], "Q");
        } else {
            board.applyMove(move[0], move[1], move[2], move[3]);
        }
    }

    private String buildChoosePromotion() {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "choose_promotion");
        return JSONUtil.encode(msg);
    }

    public synchronized void resign(ClientHandler handler) {
        board.status = "resigned";
        board.winner = (handler == white) ? Color.BLACK : Color.WHITE;
        broadcastState();
    }

    private void broadcastState() {
        String json = buildState();
        white.send(json);
        if (black != null) black.send(json);
    }

    private String buildState() {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "state");
        msg.put("board", board.toSimple());
        msg.put("turn", board.turn.label());
        msg.put("status", board.status);
        msg.put("winner", board.winner == null ? null : board.winner.label());
        msg.put("last_move", board.lastMove);
        return JSONUtil.encode(msg);
    }

    private void sendStart(ClientHandler handler, Color color, String opponent) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "game_start");
        msg.put("color", color.label());
        msg.put("opponent", opponent);
        handler.send(JSONUtil.encode(msg));
    }
}
