package chess.ai;

import chess.Board;
import chess.ChessAI;
import chess.MoveProvider;
import chess.config.AppConfig;
import chess.pieces.Piece;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiMoveProvider implements MoveProvider {

    private static final Pattern MOVE_PATTERN = Pattern.compile("(\\d+),(\\d+),(\\d+),(\\d+)");

    private final String apiKey;
    private static final String[] MODEL_FALLBACKS = {
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash"
    };

    private final String[] models;
    private final int timeoutSeconds;
    private final boolean fallbackEnabled;
    private final HttpClient httpClient;
    private final ChessAI fallback = new ChessAI(2);
    private final Consumer<String> infoCallback;

    public GeminiMoveProvider(AppConfig config, String apiKey, Consumer<String> infoCallback) {
        this.apiKey = apiKey;
        this.models = buildModelList(config.getGeminiModel());
        this.timeoutSeconds = config.getGeminiTimeoutSeconds();
        this.fallbackEnabled = config.isGeminiFallbackEnabled();
        this.infoCallback = infoCallback;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public String displayName() {
        return "Gemini";
    }

    @Override
    public int[] pickMove(Board board) {
        int[] move = requestGeminiMove(board);
        if (move != null && board.isLegalMove(move[0], move[1], move[2], move[3])) {
            return move;
        }
        if (!fallbackEnabled) return null;
        if (infoCallback != null) {
            infoCallback.accept("Gemini unavailable — local AI played this move");
        }
        System.err.println("Gemini move failed; using local AI fallback");
        return fallback.pickMove(board);
    }

    private int[] requestGeminiMove(Board board) {
        String prompt = buildPrompt(board);
        String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}],"
                + "\"generationConfig\":{\"temperature\":0.2,\"maxOutputTokens\":256}}";
        for (String model : models) {
            int[] move = requestWithModel(model, body);
            if (move != null) return move;
        }
        return null;
    }

    private int[] requestWithModel(String model, String body) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent";
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status == 200) {
                    int[] move = parseMove(response.body());
                    if (move != null) return move;
                    System.err.println("Gemini parse failed (" + model + "): "
                            + response.body().substring(0, Math.min(300, response.body().length())));
                    return null;
                }
                System.err.println("Gemini API error (" + model + "): HTTP " + status + " "
                        + response.body().substring(0, Math.min(200, response.body().length())));
                if ((status == 429 || status == 503) && attempt < 2) {
                    Thread.sleep(2000L);
                    continue;
                }
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                System.err.println("Gemini request failed (" + model + "): " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    private static String[] buildModelList(String preferred) {
        List<String> list = new ArrayList<>();
        if (preferred != null && !preferred.isBlank()) list.add(preferred.trim());
        for (String m : MODEL_FALLBACKS) {
            if (!list.contains(m)) list.add(m);
        }
        return list.toArray(new String[0]);
    }

    static int[] parseMove(String responseBody) {
        Matcher matcher = MOVE_PATTERN.matcher(responseBody);
        if (!matcher.find()) return null;
        int[] move = new int[4];
        for (int i = 0; i < 4; i++) {
            move[i] = Integer.parseInt(matcher.group(i + 1));
            if (move[i] < 0 || move[i] > 7) return null;
        }
        return move;
    }

    private String buildPrompt(Board board) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a chess engine playing as ").append(board.turn.label()).append(".\n");
        sb.append("The board uses 0-7 indices for columns (a=0) and rows (rank 1=0).\n");
        sb.append("Pieces: wK,wQ,wR,wB,wN,wP and bK,bQ,bR,bB,bN,bP. Empty cells are \".\"\n\n");
        sb.append("Current board (row 7 is rank 8, row 0 is rank 1):\n");
        String[][] grid = board.toSimple();
        for (int r = 7; r >= 0; r--) {
            for (int c = 0; c < 8; c++) {
                sb.append(grid[r][c]).append(' ');
            }
            sb.append('\n');
        }
        sb.append("\nSide to move: ").append(board.turn.label()).append("\n\n");
        sb.append("LEGAL MOVES (from_col,from_row,to_col,to_row) — you MUST pick one:\n");
        for (int[] m : allMoves(board)) {
            sb.append(m[0]).append(',').append(m[1]).append(',')
                    .append(m[2]).append(',').append(m[3]).append('\n');
        }
        sb.append("\nReply with ONLY four comma-separated integers: from_col,from_row,to_col,to_row\n");
        sb.append("No explanation, no markdown.");
        return sb.toString();
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

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(ch);
            }
        }
        return sb.toString();
    }
}
