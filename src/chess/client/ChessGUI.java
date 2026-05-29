package chess.client;

import chess.Color;
import chess.JSONUtil;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChessGUI extends JFrame {

    private static final int CELL = 70;
    private static final int BOARD_SIZE = 8 * CELL;

    private final NetworkClient net = new NetworkClient();
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private JLabel loginStatus;
    private JTextField loginUser, lobbyUserLabel;
    private JPasswordField loginPass;

    private JLabel lobbyStatusLabel;
    private JLabel gameStatusLabel;

    private JLabel whitePlayerLabel;
    private JLabel blackPlayerLabel;

    private String myColor = null;
    private String myUsername = null;
    private String opponentName = null;
    private String[][] boardData = null;
    private int[] lastMove = null;
    private int[] selectedCell = null;
    private int[][] legalMoveHints = null;
    private boolean myTurn = false;

    private BoardPanel boardPanel;
    private Timer pollTimer;

    public ChessGUI() {
        super("Chess - Bagrut Project");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        root.add(buildLoginPanel(), "login");
        root.add(buildLobbyPanel(), "lobby");
        root.add(buildGamePanel(), "game");

        add(root);
        cards.show(root, "login");
        pack();
        setLocationRelativeTo(null);

        pollTimer = new Timer(100, e -> pollMessages());
        pollTimer.start();
    }

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new java.awt.Color(40, 40, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Chess", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 32));
        title.setForeground(java.awt.Color.WHITE);
        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 0;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(styledLabel("Username:"), gbc);
        gbc.gridx = 1;
        loginUser = new JTextField(14);
        panel.add(loginUser, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(styledLabel("Password:"), gbc);
        gbc.gridx = 1;
        loginPass = new JPasswordField(14);
        panel.add(loginPass, gbc);

        JButton btnLogin = new JButton("Login");
        JButton btnRegister = new JButton("Register");
        styleButton(btnLogin, new java.awt.Color(60, 120, 60));
        styleButton(btnRegister, new java.awt.Color(60, 80, 140));
        btnLogin.addActionListener(e -> doAuth(true));
        btnRegister.addActionListener(e -> doAuth(false));
        loginPass.addActionListener(e -> doAuth(true));

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 1;
        panel.add(btnLogin, gbc);
        gbc.gridx = 1;
        panel.add(btnRegister, gbc);

        loginStatus = new JLabel(" ", SwingConstants.CENTER);
        loginStatus.setForeground(java.awt.Color.ORANGE);
        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(loginStatus, gbc);

        panel.setPreferredSize(new Dimension(340, 220));
        return panel;
    }

    private JPanel buildLobbyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new java.awt.Color(40, 40, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        lobbyUserLabel = new JTextField("Chess Lobby");
        lobbyUserLabel.setEditable(false);
        lobbyUserLabel.setBackground(new java.awt.Color(40, 40, 40));
        lobbyUserLabel.setForeground(java.awt.Color.WHITE);
        lobbyUserLabel.setFont(new Font("Serif", Font.BOLD, 20));
        lobbyUserLabel.setBorder(null);
        lobbyUserLabel.setHorizontalAlignment(JTextField.CENTER);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lobbyUserLabel, gbc);

        JButton btnHuman = new JButton("Play vs Human");
        JButton btnAI2 = new JButton("Play vs AI");
        styleButton(btnHuman, new java.awt.Color(80, 80, 160));
        styleButton(btnAI2, new java.awt.Color(140, 60, 60));

        JButton btnExit = new JButton("Exit");
        styleButton(btnExit, new java.awt.Color(120, 40, 40));

        btnHuman.addActionListener(e -> {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "play_human");
            net.send(msg);
            lobbyStatusLabel.setText("Searching for opponent...");
        });
        btnAI2.addActionListener(e -> playAI(2));
        btnExit.addActionListener(e -> {
            net.disconnect();
            System.exit(0);
        });

        gbc.gridy = 1; gbc.gridwidth = 2;
        panel.add(btnHuman, gbc);
        gbc.gridy = 2;
        panel.add(btnAI2, gbc);

        lobbyStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        lobbyStatusLabel.setForeground(java.awt.Color.CYAN);
        gbc.gridy = 3;
        panel.add(lobbyStatusLabel, gbc);

        gbc.gridy = 4;
        panel.add(btnExit, gbc);

        panel.setPreferredSize(new Dimension(340, 260));
        return panel;
    }

    private JPanel buildGamePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new java.awt.Color(30, 30, 30));

        boardPanel = new BoardPanel();
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onBoardClick(e.getX(), e.getY());
            }
        });
        panel.add(boardPanel, BorderLayout.CENTER);

        JPanel playerBar = new JPanel(new GridLayout(1, 2));
        playerBar.setBackground(new java.awt.Color(20, 20, 20));
        whitePlayerLabel = new JLabel("White", SwingConstants.CENTER);
        blackPlayerLabel = new JLabel("Black", SwingConstants.CENTER);
        whitePlayerLabel.setOpaque(true);
        blackPlayerLabel.setOpaque(true);
        whitePlayerLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        blackPlayerLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        playerBar.add(whitePlayerLabel);
        playerBar.add(blackPlayerLabel);
        panel.add(playerBar, BorderLayout.NORTH);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusBar.setBackground(new java.awt.Color(20, 20, 20));
        gameStatusLabel = new JLabel("Game in progress");
        gameStatusLabel.setForeground(java.awt.Color.WHITE);
        statusBar.add(gameStatusLabel);

        JButton btnResign = new JButton("Resign");
        styleButton(btnResign, new java.awt.Color(160, 40, 40));
        btnResign.addActionListener(e -> {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "resign");
            net.send(msg);
        });
        JButton btnLobby = new JButton("Back to Lobby");
        styleButton(btnLobby, new java.awt.Color(80, 80, 80));
        btnLobby.addActionListener(e -> {
            cards.show(root, "lobby");
            pack();
        });

        statusBar.add(btnResign);
        statusBar.add(btnLobby);
        panel.add(statusBar, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE + 90));
        return panel;
    }

    private void doAuth(boolean isLogin) {
        if (!net.connect()) {
            loginStatus.setText("Cannot connect to server (is it running?)");
            return;
        }
        String user = loginUser.getText().trim();
        String pass = new String(loginPass.getPassword()).trim();
        if (user.isEmpty() || pass.isEmpty()) {
            loginStatus.setText("Username and password cannot be empty");
            return;
        }
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", isLogin ? "login" : "register");
        msg.put("username", user);
        msg.put("password", pass);
        net.send(msg);
        loginStatus.setText("Connecting...");
    }

    private void playAI(int level) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "play_ai");
        msg.put("level", level);
        net.send(msg);
    }

    private boolean whiteView() {
        return !"black".equals(myColor);
    }

    private int colToScreenX(int col) {
        return (whiteView() ? col : 7 - col) * CELL;
    }

    private int rankToScreenY(int rank) {
        return (whiteView() ? 7 - rank : rank) * CELL;
    }

    private int screenToCol(int px) {
        int sx = px / CELL;
        return whiteView() ? sx : 7 - sx;
    }

    private int screenToRank(int py) {
        int sy = py / CELL;
        return whiteView() ? 7 - sy : sy;
    }

    private void onBoardClick(int px, int py) {
        if (boardData == null || !myTurn) return;
        int col = screenToCol(px);
        int rank = screenToRank(py);
        if (col < 0 || col > 7 || rank < 0 || rank > 7) return;

        if (selectedCell == null) {
            String cell = boardData[rank][col];
            boolean mine = (cell != null && !cell.equals(".") &&
                    cell.startsWith(whiteView() ? "w" : "b"));
            if (mine) {
                selectedCell = new int[]{col, rank};
                legalMoveHints = null;
                Map<String, Object> req = new LinkedHashMap<>();
                req.put("type", "get_moves");
                req.put("col", col);
                req.put("row", rank);
                net.send(req);
                boardPanel.repaint();
            }
        } else {
            if (col == selectedCell[0] && rank == selectedCell[1]) {
                selectedCell = null;
                legalMoveHints = null;
                boardPanel.repaint();
                return;
            }
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "move");
            msg.put("from_col", selectedCell[0]);
            msg.put("from_row", selectedCell[1]);
            msg.put("to_col", col);
            msg.put("to_row", rank);
            net.send(msg);
            selectedCell = null;
            legalMoveHints = null;
        }
    }

    private String askPromotion() {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose a piece to promote your pawn to:",
                "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        switch (choice) {
            case 1:  return "R";
            case 2:  return "B";
            case 3:  return "N";
            default: return "Q";
        }
    }

    private void pollMessages() {
        Map<String, Object> msg;
        while ((msg = net.poll()) != null) {
            handleMessage(msg);
        }
    }

    private void handleMessage(Map<String, Object> msg) {
        String type = (String) msg.get("type");
        if (type == null) return;
        switch (type) {
            case "auth_result": handleAuthResult(msg); break;
            case "info": lobbyStatusLabel.setText((String) msg.get("message")); break;
            case "error": showError((String) msg.get("message")); break;
            case "game_start": handleGameStart(msg); break;
            case "state": handleState(msg); break;
            case "moves": handleMoves(msg); break;
            case "choose_promotion": handleChoosePromotion(); break;
        }
    }

    private void handleMoves(Map<String, Object> msg) {
        Object movesObj = msg.get("moves");
        legalMoveHints = (movesObj instanceof int[][]) ? (int[][]) movesObj : null;
        boardPanel.repaint();
    }

    private void handleChoosePromotion() {
        String choice = askPromotion();
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "promote");
        msg.put("choice", choice);
        net.send(msg);
    }

    private void handleAuthResult(Map<String, Object> msg) {
        Boolean ok = (Boolean) msg.get("ok");
        String message = (String) msg.get("message");
        if (Boolean.TRUE.equals(ok)) {
            myUsername = (String) msg.get("username");
            loginStatus.setText(" ");
            lobbyUserLabel.setText("Welcome, " + myUsername);
            lobbyStatusLabel.setText(" ");
            cards.show(root, "lobby");
            pack();
        } else {
            loginStatus.setText(message);
        }
    }

    private void handleGameStart(Map<String, Object> msg) {
        myColor = (String) msg.get("color");
        opponentName = (String) msg.get("opponent");
        gameStatusLabel.setText("You are " + myColor + " vs " + opponentName);

        String whiteName = "white".equals(myColor) ? myUsername : opponentName;
        String blackName = "black".equals(myColor) ? myUsername : opponentName;
        whitePlayerLabel.setText("\u2654 " + whiteName);
        blackPlayerLabel.setText("\u265A " + blackName);

        selectedCell = null;
        boardData = null;
        cards.show(root, "game");
        pack();
    }

    private void updateActivePlayer(String turn, boolean gameOver) {
        java.awt.Color activeBg = new java.awt.Color(60, 140, 60);
        java.awt.Color idleBg = new java.awt.Color(45, 45, 45);
        boolean whiteActive = !gameOver && "white".equals(turn);
        boolean blackActive = !gameOver && "black".equals(turn);

        whitePlayerLabel.setBackground(whiteActive ? activeBg : idleBg);
        blackPlayerLabel.setBackground(blackActive ? activeBg : idleBg);
        whitePlayerLabel.setForeground(java.awt.Color.WHITE);
        blackPlayerLabel.setForeground(java.awt.Color.WHITE);
        whitePlayerLabel.setFont(whitePlayerLabel.getFont().deriveFont(whiteActive ? Font.BOLD : Font.PLAIN));
        blackPlayerLabel.setFont(blackPlayerLabel.getFont().deriveFont(blackActive ? Font.BOLD : Font.PLAIN));
    }

    private void handleState(Map<String, Object> msg) {
        boardData = (String[][]) msg.get("board");
        String turn = (String) msg.get("turn");
        String status = (String) msg.get("status");
        String winner = (String) msg.get("winner");
        Object lm = msg.get("last_move");
        lastMove = (lm instanceof int[]) ? (int[]) lm : null;
        myTurn = turn != null && turn.equals(myColor);
        if (!myTurn) { selectedCell = null; legalMoveHints = null; }
        updateActivePlayer(turn, !"playing".equals(status));

        if ("playing".equals(status)) {
            gameStatusLabel.setText(myTurn ? "Your turn (" + myColor + ")" : "Opponent's turn...");
        } else if ("checkmate".equals(status)) {
            gameStatusLabel.setText("Checkmate! " + winner + " wins!");
        } else if ("stalemate".equals(status)) {
            gameStatusLabel.setText("Stalemate – draw!");
        } else if ("resigned".equals(status)) {
            gameStatusLabel.setText(winner + " wins by resignation");
        }
        boardPanel.repaint();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(java.awt.Color.WHITE);
        return l;
    }

    private void styleButton(JButton btn, java.awt.Color bg) {
        btn.setBackground(bg);
        btn.setForeground(java.awt.Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD));
    }

    private class BoardPanel extends JPanel {

        private final String[] PIECES = {"wK","wQ","wR","wB","wN","wP","bK","bQ","bR","bB","bN","bP"};
        private final String[] SYMBOLS = {"♔","♕","♖","♗","♘","♙","♚","♛","♜","♝","♞","♟"};

        BoardPanel() {
            setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawBoard(g2);
            if (boardData != null) drawPieces(g2);
            drawHints(g2);
        }

        private void drawHints(Graphics2D g) {
            if (legalMoveHints == null) return;
            g.setColor(new java.awt.Color(0, 180, 0, 160));
            for (int[] m : legalMoveHints) {
                int px = colToScreenX(m[0]) + CELL / 2;
                int py = rankToScreenY(m[1]) + CELL / 2;
                String cell = (boardData != null) ? boardData[m[1]][m[0]] : ".";
                boolean isCapture = cell != null && !cell.equals(".");
                if (isCapture) {
                    g.setStroke(new java.awt.BasicStroke(4));
                    g.drawOval(colToScreenX(m[0]) + 4, rankToScreenY(m[1]) + 4, CELL - 8, CELL - 8);
                    g.setStroke(new java.awt.BasicStroke(1));
                } else {
                    int r = CELL / 6;
                    g.fillOval(px - r, py - r, r * 2, r * 2);
                }
            }
        }

        private void drawBoard(Graphics2D g) {
            for (int col = 0; col < 8; col++) {
                for (int rank = 0; rank < 8; rank++) {
                    int px = colToScreenX(col);
                    int py = rankToScreenY(rank);
                    boolean light = (col + rank) % 2 != 0;
                    g.setColor(light ? new java.awt.Color(240, 217, 181) : new java.awt.Color(181, 136, 99));
                    g.fillRect(px, py, CELL, CELL);

                    if (selectedCell != null && selectedCell[0] == col && selectedCell[1] == rank) {
                        g.setColor(new java.awt.Color(20, 200, 20, 140));
                        g.fillRect(px, py, CELL, CELL);
                    }
                    if (lastMove != null && lastMove.length == 4 &&
                            ((lastMove[0] == col && lastMove[1] == rank) ||
                             (lastMove[2] == col && lastMove[3] == rank))) {
                        g.setColor(new java.awt.Color(200, 200, 20, 100));
                        g.fillRect(px, py, CELL, CELL);
                    }
                }
            }

            g.setColor(new java.awt.Color(80, 80, 80));
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            String[] files = {"a","b","c","d","e","f","g","h"};
            for (int col = 0; col < 8; col++) {
                g.drawString(files[col], colToScreenX(col) + 3, BOARD_SIZE - 3);
            }
            for (int rank = 0; rank < 8; rank++) {
                g.drawString(String.valueOf(rank + 1), 3, rankToScreenY(rank) + 14);
            }
        }

        private void drawPieces(Graphics2D g) {
            g.setFont(new Font("Serif", Font.PLAIN, CELL - 10));
            FontMetrics fm = g.getFontMetrics();
            for (int rank = 0; rank < 8; rank++) {
                for (int col = 0; col < 8; col++) {
                    String cell = boardData[rank][col];
                    if (cell == null || cell.equals(".")) continue;
                    String sym = getSymbol(cell);
                    if (sym == null) continue;
                    int px = colToScreenX(col);
                    int py = rankToScreenY(rank);
                    int tx = px + (CELL - fm.stringWidth(sym)) / 2;
                    int ty = py + (CELL + fm.getAscent() - fm.getDescent()) / 2;
                    g.setColor(java.awt.Color.BLACK);
                    g.drawString(sym, tx + 1, ty + 1);
                    g.setColor(cell.startsWith("w") ? java.awt.Color.WHITE : java.awt.Color.BLACK);
                    g.drawString(sym, tx, ty);
                }
            }
        }

        private String getSymbol(String code) {
            for (int i = 0; i < PIECES.length; i++) {
                if (PIECES[i].equals(code)) return SYMBOLS[i];
            }
            return null;
        }
    }
}
