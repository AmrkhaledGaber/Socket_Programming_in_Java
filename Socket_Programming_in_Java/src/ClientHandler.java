import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.io.DataInputStream;

public class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();

                if (messageFromClient != null && messageFromClient.equals("/file")) {
                    handleFileTransfer();
                } else if (messageFromClient != null && messageFromClient.startsWith("/pm")) {
                    handlePrivateMessage(messageFromClient);
                } else if (messageFromClient != null && messageFromClient.equals("/exit")) {
                    handleExitCommand();
                    break;  // Break out of the loop when receiving "/exit"
                } else if (messageFromClient != null) {
                    broadcastMessage(messageFromClient);
                }
            } catch (IOException e) {
                closeEverything();
                break;
            }
        }
    }

    private void handleFileTransfer() {
        try {
            String fileName = bufferedReader.readLine();
            long fileSize = Long.parseLong(bufferedReader.readLine());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            System.out.println("Receiving file: " + fileName + " (" + fileSize + " bytes)");

            String savePath = "C:\\Users\\AHMED DAWOD\\IdeaProjects\\untitled3\\savedfiles\\" + fileName;

            try (FileOutputStream fos = new FileOutputStream(savePath);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    fileSize -= bytesRead;
                }

                System.out.println("File received successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePrivateMessage(String privateMessage) {
        String[] parts = privateMessage.split(" ", 3);
        if (parts.length == 3) {
            String recipient = parts[1];
            String message = parts[2];
            sendPrivateMessage(recipient, clientUsername + " (private): " + message);
        } else {
            sendServerMessage("Invalid private message format. Use: /pm <recipient> <message>");
        }
    }

    // Add this method to send server messages to the client
    private void sendServerMessage(String message) {
        try {
            bufferedWriter.write("Server: " + message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    private void sendPrivateMessage(String recipient, String privateMessage) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (clientHandler != this && clientHandler.clientUsername.equals(recipient)) {
                    clientHandler.bufferedWriter.write(privateMessage);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                    // Notify the sender that the message has been sent
                    sendServerMessage("Private message sent to " + recipient + ": " + privateMessage);
                    return;
                }
            } catch (IOException e) {
                closeEverything();
            }
        }

        // Notify the sender that the recipient is not found or offline
        sendServerMessage("User '" + recipient + "' not found or offline.");
    }
    private void handlePrivateMessageInput(String message) {
        // Extract recipient and message from /pm command
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
            String recipient = parts[1];
            String privateMessage = parts[2];

            // Send the private message
            sendPrivateMessage(recipient, privateMessage);
        } else {
            System.out.println("Invalid private message format. Use: /pm <recipient> <message>");
        }
    }
    private void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (clientHandler != this) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();  // Ensure proper flushing
                }
            } catch (IOException e) {
                // Handle or log the exception
                e.printStackTrace();
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
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleExitCommand() {
        broadcastMessage("Server: " + clientUsername + " has left the chat!");
        clientHandlers.remove(this);
        closeEverything();
    }
}
