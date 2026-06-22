package chess.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private final Properties props = new Properties();

    public static AppConfig load(String path) {
        AppConfig config = new AppConfig();
        try (InputStream in = new FileInputStream(path)) {
            config.props.load(in);
        } catch (IOException ignored) {
        }
        return config;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String env = envOverride(key);
        if (env != null) return Boolean.parseBoolean(env);
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value.trim());
    }

    public String getString(String key, String defaultValue) {
        String env = envOverride(key);
        if (env != null) return env;
        return props.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String env = envOverride(key);
        if (env != null) {
            try {
                return Integer.parseInt(env.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public String getKeystorePath() {
        String env = System.getenv("CHESS_KEYSTORE");
        if (env != null && !env.isEmpty()) return env;
        return getString("chess.tls.keystore", "config/server-keystore.jks");
    }

    public String getKeystorePassword() {
        String env = System.getenv("CHESS_KEYSTORE_PASSWORD");
        if (env != null) return env;
        return getString("chess.tls.keystore.password", "changeit");
    }

    public String getTruststorePath() {
        String env = System.getenv("CHESS_TRUSTSTORE");
        if (env != null && !env.isEmpty()) return env;
        return getString("chess.tls.truststore", "config/client-truststore.jks");
    }

    public String getTruststorePassword() {
        String env = System.getenv("CHESS_TRUSTSTORE_PASSWORD");
        if (env != null) return env;
        return getString("chess.tls.truststore.password", "changeit");
    }

    public String getServerHost() {
        return getString("chess.server.host", "127.0.0.1");
    }

    public int getServerPort() {
        return getInt("chess.server.port", 5555);
    }

    public String getGeminiModel() {
        return getString("chess.gemini.model", "gemini-2.5-flash");
    }

    public int getGeminiTimeoutSeconds() {
        return getInt("chess.gemini.timeout.seconds", 10);
    }

    private String envOverride(String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        return System.getenv(envKey);
    }
}
