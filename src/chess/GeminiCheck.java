package chess;

import chess.ai.GeminiMoveProvider;
import chess.config.AppConfig;
import chess.config.GeminiApiKey;

public class GeminiCheck {

    public static void main(String[] args) {
        String apiKey = GeminiApiKey.resolve();
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("[FAIL] GOOGLE_API_KEY not set");
            System.exit(1);
        }
        AppConfig config = AppConfig.load("config/server.properties");
        GeminiMoveProvider provider = new GeminiMoveProvider(config, apiKey, msg -> System.out.println("[info] " + msg));

        Board board = new Board();
        board.applyMove(4, 1, 4, 3);
        System.out.println("Test position: after 1.e4, Black to move");
        System.out.println("Calling Gemini API...");

        long start = System.currentTimeMillis();
        int[] move = provider.pickMove(board);
        long ms = System.currentTimeMillis() - start;

        if (move == null) {
            System.out.println("[FAIL] No move returned (" + ms + " ms)");
            System.exit(1);
        }
        boolean legal = board.isLegalMove(move[0], move[1], move[2], move[3]);
        System.out.println("[OK] Move: " + move[0] + "," + move[1] + " -> " + move[2] + "," + move[3]
                + " | legal=" + legal + " | " + ms + " ms");
        if (!legal) {
            System.out.println("[WARN] Move was not legal - fallback may have been used");
        }
        System.exit(legal ? 0 : 2);
    }
}
