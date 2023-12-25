import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.ArrayList;

public class SecureClientHandler implements Runnable {
    public static ArrayList<SecureClientHandler> clientHandlers = new ArrayList<>();
    private SSLSocket sslSocket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public SecureClientHandler(SSLSocket sslSocket) {
        try {
            this.sslSocket = sslSocket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            clientHandlers.add(this);
            broadcastMessage("Server: " + clientUsername + " has entered the chat!");
        } catch (IOException e) {
            closeEverything();
        }
    }

    @Override
    public void run() {
        String messageFromClient;
        try {
            while (sslSocket.isConnected()) {
                messageFromClient = bufferedReader.readLine();

                if (messageFromClient != null) {
                    if (messageFromClient.equals("/exit")) {
                        handleExitCommand();
                        break;
                    } else if (messageFromClient.startsWith("/pm")) {
                        handlePrivateMessage(messageFromClient);
                    } else {
                        broadcastMessage(clientUsername + " : " + messageFromClient);
                    }
                }
            }
        } catch (IOException e) {
            closeEverything();
        }
    }

    private void handleExitCommand() {
        broadcastMessage("Server: " + clientUsername + " has left the chat!");
        closeEverything();
    }

    private void handlePrivateMessage(String privateMessage) {
        String[] parts = privateMessage.split(" ", 3);
        if (parts.length == 3) {
            String recipient = parts[1];
            String message = parts[2];
            sendPrivateMessage(recipient, clientUsername + " (private): " + message);
        } else {
            System.out.println("Invalid private message format. Use: /pm <recipient> <message>");
        }
    }

    private void sendPrivateMessage(String recipient, String privateMessage) {
        for (SecureClientHandler clientHandler : clientHandlers) {
            try {
                if (clientHandler != this && clientHandler.clientUsername.equals(recipient)) {
                    clientHandler.bufferedWriter.write(privateMessage);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                    return;
                }
            } catch (IOException e) {
                closeEverything();
            }
        }

        try {
            bufferedWriter.write("Server: User '" + recipient + "' not found or offline.");
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    private void broadcastMessage(String messageToSend) {
        for (SecureClientHandler clientHandler : clientHandlers) {
            try {
                if (clientHandler != this) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything();
            }
        }
    }

    private void closeEverything() {
        clientHandlers.remove(this);
        broadcastMessage("Server: " + clientUsername + " has left the chat!");

        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (sslSocket != null && !sslSocket.isClosed()) {
                sslSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
