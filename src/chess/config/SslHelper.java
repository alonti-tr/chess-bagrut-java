package chess.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

public final class SslHelper {

    private SslHelper() {}

    public static ServerSocket createServerSocket(int port, AppConfig config) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        KeyStore ks = KeyStore.getInstance("JKS");
        char[] password = config.getKeystorePassword().toCharArray();
        try (FileInputStream in = new FileInputStream(config.getKeystorePath())) {
            ks.load(in, password);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);
        ctx.init(kmf.getKeyManagers(), null, null);
        SSLServerSocket server = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(port);
        return server;
    }

    public static Socket createClientSocket(String host, int port, AppConfig config) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        KeyStore ts = KeyStore.getInstance("JKS");
        char[] password = config.getTruststorePassword().toCharArray();
        try (FileInputStream in = new FileInputStream(config.getTruststorePath())) {
            ts.load(in, password);
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        ctx.init(null, tmf.getTrustManagers(), null);
        return (SSLSocket) ctx.getSocketFactory().createSocket(host, port);
    }
}
