package chess;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class UserAuth {

    private static final String USERS_FILE = "users.json";
    private final Map<String, String[]> users = new HashMap<>();

    public UserAuth() {
        load();
    }

    public synchronized boolean register(String username, String password) {
        if (users.containsKey(username)) return false;
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        String salt = Base64.getEncoder().encodeToString(saltBytes);
        String hash = hash(password, salt);
        users.put(username, new String[]{salt, hash});
        save();
        return true;
    }

    public synchronized boolean login(String username, String password) {
        String[] entry = users.get(username);
        if (entry == null) return false;
        return entry[1].equals(hash(password, entry[0]));
    }

    private String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = salt + password;
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void load() {
        File f = new File(USERS_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            parseJson(sb.toString());
        } catch (IOException ignored) {}
    }

    private void parseJson(String json) {
        json = json.trim();
        if (json.length() < 2) return;
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return;

        String[] entries = json.split(",(?=\")");
        for (String entry : entries) {
            entry = entry.trim();
            int colon = entry.indexOf(':');
            if (colon < 0) continue;
            String name = entry.substring(0, colon).trim().replace("\"", "");
            String val = entry.substring(colon + 1).trim();
            val = val.replace("\"", "");
            String[] parts = val.split(":");
            if (parts.length == 2) users.put(name, new String[]{parts[0], parts[1]});
        }
    }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USERS_FILE))) {
            pw.print("{");
            boolean first = true;
            for (Map.Entry<String, String[]> e : users.entrySet()) {
                if (!first) pw.print(",");
                pw.printf("\"%s\":\"%s:%s\"", e.getKey(), e.getValue()[0], e.getValue()[1]);
                first = false;
            }
            pw.print("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
