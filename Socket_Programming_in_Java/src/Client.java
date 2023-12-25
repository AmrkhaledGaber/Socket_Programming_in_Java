import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;

            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            // Start a new thread for user input
            new Thread(this::handleUserInput).start();

            // Start a new thread to listen for messages
            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    private void handleUserInput() {
        try {
            Scanner scanner = new Scanner(System.in);
            while (socket != null && socket.isConnected()) {
                System.out.print("Enter your message (Type '/help' for instructions): ");
                String messageToSend = scanner.nextLine();

                if (messageToSend.startsWith("/pm")) {
                    // Handle private messages
                    handlePrivateMessageInput(messageToSend);

                } else if (messageToSend.equals("/file")) {
                    selectFile();
                } else if (messageToSend.equals("/exit")) {
                    exitChat();
                    break;
                } else if (messageToSend.equals("/help")) {
                    displayInstructions();
                } else {
                    sendMessage(username + " : " + messageToSend);
                }
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    private void listenForMessages() {
        try {
            while (socket != null && socket.isConnected()) {
                String msgFromGroupChat = bufferedReader.readLine();
                if (msgFromGroupChat == null) {
                    break;
                }
                System.out.println(msgFromGroupChat);
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
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

    private void sendPrivateMessage(String recipient, String privateMessage) {
        try {
            sendMessage("/pm " + recipient + " " + privateMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) throws IOException {
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    private void displayInstructions() {
        System.out.println("----- Instructions -----");
        System.out.println("/pm <recipient> <message>: Send a private message");
        System.out.println("/file: Send a file");
        System.out.println("/exit: Exit the chat");
        System.out.println("/help: Display instructions");
        System.out.println("------------------------");
    }

    private void exitChat() {
        try {
            sendMessage("/exit");
            closeEverything(socket, bufferedReader, bufferedWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void selectFile() {
        try {
            System.out.print("Enter the path of the file to send: ");
            Scanner scanner = new Scanner(System.in);
            String filePath = scanner.nextLine();

            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                sendFile(file);
            } else {
                System.out.println("Invalid file path or file does not exist.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFile(File file) {
        try {
            String fileName = file.getName();
            long fileSize = file.length();

            sendMessage("/file");
            sendMessage(fileName);
            sendMessage(Long.toString(fileSize));

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                bufferedWriter.write(Arrays.toString(buffer), 0, bytesRead);
                bufferedWriter.flush();
            }

            fis.close();
            System.out.println("File sent successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
            System.exit(0); // Terminate the application
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter server address: ");
            String serverAddress = scanner.nextLine();

            System.out.print("Enter server port: ");
            int serverPort = scanner.nextInt();
            scanner.nextLine(); // Consume the newline character

            System.out.print("Enter your nickname for the chat: ");
            String username = scanner.nextLine();

            Socket socket = new Socket(serverAddress, serverPort);
            Client client = new Client(socket, username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
