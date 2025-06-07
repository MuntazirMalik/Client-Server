import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.SwingUtilities;
import javax.swing.*;

/**
 * Simple chat client GUI that connects to the chat server.
 * Handles sending messages to and receiving messages from the server.
 */
public class ChatClient3 extends JFrame {

    private final JTextArea chatArea;        // Displays chat history
    private final JTextField messageField;   // User input for messages
    private final JButton sendButton;        // Button to send messages

    private Socket socket;             // Socket connection to server
    private Scanner input;             // Reads messages from server
    private PrintWriter output;        // Sends messages to server

    public ChatClient3() {
        setTitle("Chat Client");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize GUI components
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Send");

        // Send message when button clicked
        sendButton.addActionListener(e -> sendMessage());

        // Layout panel for message input and send button
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(messageField);
        panel.add(sendButton);

        add(new JLabel("Client"), "North");
        add(scrollPane, "Center");
        add(panel, "South");

        // Connect to server when window opens
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent evt) {
                connectToServer();
            }

            // Clean up resources on close
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeConnection();
            }
        });
    }

    /**
     * Connects to the chat server running on 192.168.18.17 port 4789.
     * Sets up input and output streams for communication.
     * Starts a background thread to listen for incoming messages.
     */
    private void connectToServer() {
        try {
            // Create socket connected to server IP and port
            socket = new Socket("192.168.18.17", 4789);

            // Input stream to receive server messages
            input = new Scanner(socket.getInputStream());

            // Output stream to send messages to server
            output = new PrintWriter(socket.getOutputStream(), true);

            // Start thread to listen for server messages continuously
            new Thread(() -> {
                try {
                    while (input.hasNextLine()) {
                        String message = input.nextLine();

                        // Update chat area in the Swing Event Dispatch Thread (EDT)
                        SwingUtilities.invokeLater(() -> chatArea.append("Server: " + message + "\n"));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> chatArea.append("Disconnected from server.\n"));
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not connect to server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sends the text entered in the messageField to the server,
     * then displays it in the chat area.
     */
    private void sendMessage() {
        String msg = messageField.getText().trim();

        if (!msg.isEmpty() && output != null) {
            output.println(msg); // Send to server
            chatArea.append("Client: " + msg + "\n"); // Display locally

            // Shutdown command
            if (msg.equalsIgnoreCase("shutdown")) {
                closeConnection(); // Optionally close client after sending shutdown
            }

            messageField.setText(""); // Clear input
        }
    }

    /**
     * Closes all streams and the socket connection safely.
     */
    private void closeConnection() {
        try {
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Main method to start the client GUI.
     * @param args
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient3().setVisible(true));
    }
}
