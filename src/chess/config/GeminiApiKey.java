package chess.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public final class GeminiApiKey {

    private static final String KEY_FILE = "config/gemini.key";

    private GeminiApiKey() {}

    public static String resolve() {
        String env = System.getenv("GOOGLE_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        return readKeyFile();
    }

    private static String readKeyFile() {
        File file = new File(KEY_FILE);
        if (!file.isFile()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) return line;
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
