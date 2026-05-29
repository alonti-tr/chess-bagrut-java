package chess.client;

import chess.JSONUtil;
import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkClient {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5555;

    private Socket socket;
    private PrintWriter out;
    private final BlockingQueue<Map<String, Object>> inbox = new LinkedBlockingQueue<>();

    public boolean connect() {
        if (socket != null && socket.isConnected() && !socket.isClosed()) return true;
        try {
            socket = new Socket(HOST, PORT);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        inbox.put(JSONUtil.decode(line));
                    }
                } catch (Exception ignored) {}
            });
            reader.setDaemon(true);
            reader.start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void send(Map<String, Object> msg) {
        if (out != null) out.println(JSONUtil.encode(msg));
    }

    public Map<String, Object> poll() {
        return inbox.poll();
    }

    public void disconnect() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
