package chess.server;

import chess.ChessAI;
import chess.JSONUtil;
import chess.UserAuth;
import java.io.*;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final UserAuth auth;
    private final Matchmaker matchmaker;
    private final PrintWriter out;
    private final BufferedReader in;

    private String username = null;
    private Game game = null;
    private boolean inQueue = false;

    public ClientHandler(Socket socket, UserAuth auth, Matchmaker matchmaker) throws IOException {
        this.socket = socket;
        this.auth = auth;
        this.matchmaker = matchmaker;
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
    }

    public String getUsername() { return username; }

    public synchronized void send(String json) {
        out.println(json);
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(JSONUtil.decode(line));
            }
        } catch (IOException ignored) {
        } finally {
            cleanup();
        }
    }

    private void handleMessage(Map<String, Object> msg) {
        String type = (String) msg.get("type");
        if (type == null) return;

        switch (type) {
            case "register": handleAuth(msg, false); break;
            case "login":    handleAuth(msg, true);  break;
            case "play_human": handlePlayHuman(); break;
            case "play_ai":    handlePlayAI(msg); break;
            case "cancel_wait": handleCancelWait(); break;
            case "get_moves": handleGetMoves(msg); break;
            case "move":   handleMove(msg); break;
            case "promote": handlePromote(msg); break;
            case "resign": handleResign(); break;
        }
    }

    private void handleAuth(Map<String, Object> msg, boolean isLogin) {
        String user = (String) msg.get("username");
        String pass = (String) msg.get("password");
        if (user == null || pass == null || user.isEmpty() || pass.isEmpty()) {
            sendError("Username and password are required");
            return;
        }
        boolean ok = isLogin ? auth.login(user, pass) : auth.register(user, pass);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("type", "auth_result");
        resp.put("ok", ok);
        if (ok) {
            username = user;
            resp.put("message", isLogin ? "Welcome back, " + user + "!" : "Registered and logged in, " + user + "!");
            resp.put("username", username);
        } else {
            resp.put("message", isLogin ? "Wrong username or password" : "Username already taken");
        }
        send(JSONUtil.encode(resp));
    }

    private void handlePlayHuman() {
        if (!loggedIn()) return;
        inQueue = true;
        sendInfo("Looking for an opponent...");
        ClientHandler opponent = matchmaker.tryMatch(this);
        if (opponent != null) {
            inQueue = false;
            opponent.inQueue = false;
            Game g = new Game(opponent, this, null);
            opponent.game = g;
            this.game = g;
            g.start();
        }
    }

    private void handlePlayAI(Map<String, Object> msg) {
        if (!loggedIn()) return;
        Object lvlObj = msg.get("level");
        int level = (lvlObj instanceof Integer) ? (Integer) lvlObj : 1;
        Game g = new Game(this, null, new ChessAI(level));
        this.game = g;
        g.start();
    }

    private void handleCancelWait() {
        if (inQueue) {
            matchmaker.remove(this);
            inQueue = false;
            sendInfo("Cancelled matchmaking");
        }
    }

    private void handleGetMoves(Map<String, Object> msg) {
        if (game == null) return;
        try {
            int col = ((Number) msg.get("col")).intValue();
            int row = ((Number) msg.get("row")).intValue();
            int[][] moves = game.getLegalMoves(this, col, row);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("type", "moves");
            resp.put("col", col);
            resp.put("row", row);
            resp.put("moves", moves);
            send(JSONUtil.encode(resp));
        } catch (Exception ignored) {}
    }

    private void handleMove(Map<String, Object> msg) {
        if (game == null) return;
        try {
            int fc = ((Number) msg.get("from_col")).intValue();
            int fr = ((Number) msg.get("from_row")).intValue();
            int tc = ((Number) msg.get("to_col")).intValue();
            int tr = ((Number) msg.get("to_row")).intValue();
            boolean ok = game.applyMove(this, fc, fr, tc, tr);
            if (!ok) sendError("Illegal move");
        } catch (Exception e) {
            sendError("Invalid move data");
        }
    }

    private void handlePromote(Map<String, Object> msg) {
        if (game == null) return;
        Object choiceObj = msg.get("choice");
        String choice = (choiceObj instanceof String) ? (String) choiceObj : "Q";
        game.completePromotion(this, choice);
    }

    private void handleResign() {
        if (game != null) game.resign(this);
    }

    private boolean loggedIn() {
        if (username == null) { sendError("Not logged in"); return false; }
        return true;
    }

    private void sendInfo(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "info");
        m.put("message", msg);
        send(JSONUtil.encode(m));
    }

    private void sendError(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "error");
        m.put("message", msg);
        send(JSONUtil.encode(m));
    }

    private void cleanup() {
        if (inQueue) matchmaker.remove(this);
        try { socket.close(); } catch (IOException ignored) {}
    }
}
