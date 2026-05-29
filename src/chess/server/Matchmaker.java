package chess.server;

import java.util.LinkedList;
import java.util.Queue;

public class Matchmaker {

    private final Queue<ClientHandler> queue = new LinkedList<>();

    public synchronized ClientHandler tryMatch(ClientHandler client) {
        if (!queue.isEmpty()) {
            return queue.poll();
        }
        queue.add(client);
        return null;
    }

    public synchronized boolean remove(ClientHandler client) {
        return queue.remove(client);
    }
}
