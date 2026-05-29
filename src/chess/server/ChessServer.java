package chess.server;

import chess.UserAuth;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChessServer {

    private static final int PORT = 5555;
    private final UserAuth auth = new UserAuth();
    private final Matchmaker matchmaker = new Matchmaker();

    public void start() throws IOException {
        System.out.println("Chess server started on port " + PORT);
        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = server.accept();
                System.out.println("New connection from " + socket.getInetAddress());
                try {
                    ClientHandler handler = new ClientHandler(socket, auth, matchmaker);
                    new Thread(handler).start();
                } catch (IOException e) {
                    System.err.println("Failed to create handler: " + e.getMessage());
                }
            }
        }
    }
}
