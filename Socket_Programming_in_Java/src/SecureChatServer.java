import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.io.File;

public class SecureChatServer {
    private SSLServerSocket sslServerSocket;

    public SecureChatServer(SSLServerSocket sslServerSocket) {
        this.sslServerSocket = sslServerSocket;
    }

    public void startServer() {
        try {
            while (!sslServerSocket.isClosed()) {
                SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                System.out.println("A new client has connected");
                SecureClientHandler clientHandler = new SecureClientHandler(sslSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeServerSocket() {
        try {
            if (sslServerSocket != null && !sslServerSocket.isClosed()) {
                sslServerSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = 8989;

        char[] keystorePassword = "962003".toCharArray();
        String keystorePath = "C:\\Users\\AHMED DAWOD\\IdeaProjects\\untitled3\\src\\server.jks";

        try {
            System.out.println("Absolute path to keystore file: " + new File(keystorePath).getAbsolutePath());

            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream fileInputStream = new FileInputStream(keystorePath)) {
                keyStore.load(fileInputStream, keystorePassword);
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, keystorePassword);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);

            SecureChatServer server = new SecureChatServer(sslServerSocket);
            server.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
