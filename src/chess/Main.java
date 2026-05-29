package chess;

import chess.client.ChessGUI;
import chess.server.ChessServer;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) throws Exception {
        String mode = (args.length > 0) ? args[0] : "client";
        if ("server".equals(mode)) {
            new ChessServer().start();
        } else {
            SwingUtilities.invokeLater(() -> {
                ChessGUI gui = new ChessGUI();
                gui.setVisible(true);
            });
        }
    }
}
