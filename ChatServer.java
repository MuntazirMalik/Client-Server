import java.awt.BorderLayout;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class ChatServer extends JFrame {
    // Text area for server to view chat messages
    private final JTextArea chatArea;

    // Input field and send button for manual server messages
    private final JTextField inputField;
    private final JButton sendButton;

    // ✅ Broadcast toggle
    private final JCheckBox broadcastToggle;

    // Server socket to listen for incoming TCP client connections
    private ServerSocket serverSocket;

    // Thread-safe set to store all connected clients
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public ChatServer() {
        setTitle("Chat Server");
        setSize(400, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center window

        // Create and configure GUI components
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("Send");
        broadcastToggle = new JCheckBox("Broadcast Enabled", true); // ✅ Checkbox

        // Panel to hold input field and button
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(broadcastToggle, BorderLayout.SOUTH); // ✅ Add checkbox

        // Add components to the window
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Send button and Enter key trigger manual message
        sendButton.addActionListener(e -> sendManualMessage());
        inputField.addActionListener(e -> sendManualMessage());

        // Start/stop server on window open/close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent evt) {
                startServer();
            }

            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                stopServer();
            }
        });
    }

    /**
     * Sends a message typed into the input field to all connected clients.
     */
    private void sendManualMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            appendToChat("Server: " + message); // Show in server window
            if (broadcastToggle.isSelected()) { // ✅ Only send if checkbox is checked
                broadcastMessage(message);      // Send to clients
            }
            inputField.setText("");
        }
    }

    /**
     * Sends a message to all connected clients.
     */
    private void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    /**
     * Starts the server on TCP port 4789 and accepts incoming client connections.
     */
    private void startServer() {
        new Thread(() -> {
            try {
                // Bind server to port 4789 on the local IP address
                serverSocket = new ServerSocket(4789);
                String ip = InetAddress.getLocalHost().getHostAddress();
                appendToChat("Server started on IP: " + ip + ", Port: 4789");

                // Accept new clients in a loop
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    appendToChat("Client connected from: " + clientSocket.getInetAddress().getHostAddress());

                    // Handle client in separate thread
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                appendToChat("Server stopped.");
            }
        }).start();
    }

    /**
     * Stops the server and disconnects all clients.
     */
    private void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            for (ClientHandler client : clients) {
                client.stop();
            }
        } catch (IOException e) {
            appendToChat("Error stopping server.");
        }
    }

    /**
     * Adds a message to the server chat window (thread-safe).
     */
    private void appendToChat(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    /**
     * Inner class to handle a single client's communication.
     */
    private class ClientHandler implements Runnable {
        private Socket socket;
        private Scanner input;
        private PrintWriter output;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                input = new Scanner(socket.getInputStream());
                output = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                appendToChat("Error setting up client I/O streams.");
            }
        }

        @Override
        public void run() {
            try {
                // Read messages from this client
                while (input.hasNextLine()) {
                    String message = input.nextLine();
                    appendToChat("Client says: " + message);

                    // Auto-reply back to client
                    String response = generateReply(message);
                    output.println(response);

                    // Optional: broadcast message from this client to others
                    // broadcastMessage("Client: " + message);
                }
            } catch (Exception e) {
                appendToChat("Client disconnected: " + socket.getInetAddress().getHostAddress());
            } finally {
                stop();
            }
        }

        /**
         * Sends a message to this client.
         */
        public void sendMessage(String message) {
            if (output != null) {
                output.println(message);
            }
        }

        /**
         * Gracefully stops this client connection.
         */
        public void stop() {
            try {
                if (output != null) output.close();
                if (input != null) input.close();
                if (socket != null && !socket.isClosed()) socket.close();
                clients.remove(this);
            } catch (IOException ignored) {}
        }

        /**
         * Simple logic to generate automatic replies.
         */
        private String generateReply(String msg) {
            msg = msg.toLowerCase().trim();

            if (msg.contains("hello") || msg.contains("hi")) {
                return "Hello! How can I assist you?";
            } else if (msg.contains("how are you")) {
                return "I'm a server, always running!";
            } else if (msg.contains("bye") || msg.contains("exit")) {
                return "Goodbye!";
            } else if (containsSpecialCharacters(msg)) {
                return "Message not understood.";
            } else {
                return "You said: '" + msg + "'";
            }
        }

        /**
         * Detects if message contains symbols/punctuation.
         */
        private boolean containsSpecialCharacters(String s) {
            return s.matches(".*[!@#$%^&*()_+=\\[\\]{};:'\",.<>/?`~].*");
        }
    }

    /**
     * Launches the chat server window.
     * @param args
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatServer().setVisible(true));
    }
}
