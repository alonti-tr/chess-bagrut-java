package chess.server;

import chess.UserAuth;
import chess.config.AppConfig;
import chess.config.SslHelper;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChessServer {

    private static final String CONFIG_PATH = "config/server.properties";
    private final AppConfig config = AppConfig.load(CONFIG_PATH);
    private final UserAuth auth = new UserAuth();
    private final Matchmaker matchmaker = new Matchmaker();

    public void start() throws Exception {
        int port = config.getServerPort();
        System.out.println("Chess server started on port " + port + " (TLS)");
        ServerSocket server = SslHelper.createServerSocket(port, config);
        try (server) {
            while (true) {
                Socket socket = server.accept();
                System.out.println("New connection from " + socket.getInetAddress());
                try {
                    ClientHandler handler = new ClientHandler(socket, auth, matchmaker, config);
                    new Thread(handler).start();
                } catch (IOException e) {
                    System.err.println("Failed to create handler: " + e.getMessage());
                }
            }
        }
    }
}
